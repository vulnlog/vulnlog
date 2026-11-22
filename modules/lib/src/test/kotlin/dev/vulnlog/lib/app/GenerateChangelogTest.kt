// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.app.report.ChangelogFilterRejected
import dev.vulnlog.lib.app.report.ChangelogFormat
import dev.vulnlog.lib.app.report.ChangelogOutcome
import dev.vulnlog.lib.app.report.ChangelogProjectsDiffer
import dev.vulnlog.lib.app.report.ChangelogRequest
import dev.vulnlog.lib.app.report.generateChangelog
import dev.vulnlog.lib.codec.changelog.ChangelogEncoder
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ChangelogReport
import dev.vulnlog.lib.document.validated
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

private val ACME = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")
private val GENERATED_AT: Instant = Instant.parse("2026-05-02T10:30:00Z")

/** Reported for 1.0.0, so users ran it before the release that fixed it. */
private fun fixedIn(
    release: String,
    id: String = "CVE-2026-0001",
): VulnerabilityEntry =
    vulnerability(
        id = cve(id),
        releases = listOf(release("1.0.0")),
        verdict = Verdict.Affected(Severity.HIGH),
        resolution = resolution(release),
        analysis = "reviewed",
    )

/** Through YAML and the load step, as a driver hands it over: only the load step builds a project. */
private fun loaded(
    vararg vulnerabilities: VulnerabilityEntry,
    project: Project = ACME,
): ValidVulnlogProject =
    validated(
        YamlWriter.write(
            vulnlogFile(
                project = project,
                releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0"), releaseEntry("3.0.0")),
                vulnerabilities = vulnerabilities.toList(),
            ),
        ),
    )

private fun generate(
    vararg projects: ValidVulnlogProject,
    filter: FilterRequest = FilterRequest(),
    format: ChangelogFormat = ChangelogFormat.TEXT,
): ChangelogOutcome =
    generateChangelog(
        projects.toList(),
        ChangelogRequest(filter, format, ChangelogDetail.FULL, GENERATED_AT, "1.2.3-test"),
    )

class GenerateChangelogTest :
    FunSpec({

        test("lists what each release fixed, encoded in the requested format") {
            val project = loaded(fixedIn("2.0.0", "CVE-2026-0001"), fixedIn("3.0.0", "CVE-2026-0002"))

            val outcome = generate(project, format = ChangelogFormat.MARKDOWN)

            val fixed = outcome.shouldBeInstanceOf<ChangelogOutcome.Fixed>()
            fixed.changelog.releases.map { it.fixedIn } shouldContainExactly listOf(release("3.0.0"), release("2.0.0"))
            fixed.content shouldBe ChangelogEncoder.encodeMarkdown(fixed.changelog, ChangelogDetail.FULL)
        }

        test("encodes JSON with the run that generated it") {
            val project = loaded(fixedIn("2.0.0"))
            val filter = FilterRequest(asOf = "2.0.0")

            val outcome = generate(project, filter = filter, format = ChangelogFormat.JSON)

            val fixed = outcome.shouldBeInstanceOf<ChangelogOutcome.Fixed>()
            val report =
                ChangelogReport(
                    changelog = fixed.changelog,
                    generatedAt = GENERATED_AT,
                    vulnlogVersion = "1.2.3-test",
                    inputs = listOf(project.inputDocument.filename),
                    filter = fixed.filter,
                    asOf = release("2.0.0"),
                )
            fixed.content shouldBe ChangelogEncoder.encodeJson(report)
        }

        test("applies the filter before collecting") {
            val project = loaded(fixedIn("2.0.0", "CVE-2026-0001"), fixedIn("3.0.0", "CVE-2026-0002"))

            val outcome = generate(project, filter = FilterRequest(fixedIn = "2.0.0"))

            val fixed = outcome.shouldBeInstanceOf<ChangelogOutcome.Fixed>()
            fixed.filter shouldBe ResolvedFilter(fixedIn = release("2.0.0"))
            fixed.changelog.releases.map { it.fixedIn } shouldContainExactly listOf(release("2.0.0"))
        }

        test("a scope without fixes still encodes the empty changelog") {
            val project = loaded(vulnerability(id = cve("CVE-2026-0001"), releases = listOf(release("1.0.0"))))

            val outcome = generate(project)

            val nothing = outcome.shouldBeInstanceOf<ChangelogOutcome.NothingFixed>()
            nothing.changelog.releases.shouldBeEmpty()
            nothing.content shouldBe ChangelogEncoder.encodeText(nothing.changelog, ChangelogDetail.FULL)
        }

        test("refuses files of different projects, before it looks at the filter") {
            val other = Project("Other Corp", "Other App", "Other Security Team")
            val acme = loaded(fixedIn("2.0.0"))
            val otherProject = loaded(fixedIn("2.0.0"), project = other)

            val outcome = generate(acme, otherProject, filter = FilterRequest(fixedIn = "9.9.9"))

            outcome shouldBe ChangelogProjectsDiffer(listOf(ACME, other))
        }

        test("rejects a release the files do not declare") {
            val project = loaded(fixedIn("2.0.0"))

            val outcome = generate(project, filter = FilterRequest(fixedIn = "9.9.9"))

            outcome shouldBe
                ChangelogFilterRejected(
                    listOf(
                        FilterProblem.UnknownRelease(
                            release("9.9.9"),
                            listOf(release("1.0.0"), release("2.0.0"), release("3.0.0")),
                        ),
                    ),
                )
        }
    })
