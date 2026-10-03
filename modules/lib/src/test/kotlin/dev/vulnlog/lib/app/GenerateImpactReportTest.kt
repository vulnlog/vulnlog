// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.impact.ImpactReportEncoder
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

private val GENERATED_AT = Instant.parse("2026-05-02T10:30:00Z")
private val ACME = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")
private val NOT_AFFECTED = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH)

private fun fileWith(
    id: String,
    verdict: Verdict = Verdict.Affected(Severity.HIGH),
    resolution: Resolution? = null,
    project: Project = ACME,
): VulnlogFile =
    vulnlogFile(
        project = project,
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")),
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve(id),
                    releases = listOf(release("1.0.0")),
                    verdict = verdict,
                    resolution = resolution,
                    analysis = "reviewed",
                ),
            ),
    )

/** Through YAML and the load step, as a driver hands it over: only the load step builds a project. */
private fun loaded(
    file: VulnlogFile,
    name: String = "app.vl.yaml",
): ValidVulnlogProject =
    validateDocument(InputDocument(YamlWriter.write(file), name))
        .shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
        .project

private fun generate(
    vararg projects: ValidVulnlogProject,
    filter: FilterRequest = FilterRequest(),
): ImpactReportOutcome =
    generateImpactReport(projects.toList(), ImpactReportRequest(filter, GENERATED_AT, "1.2.3-test"))

private fun ImpactReportOutcome.generated(): ImpactReportOutcome.Generated = shouldBeInstanceOf()

class GenerateImpactReportTest :
    FunSpec({

        test("merges the entries of every file into one row per vulnerability") {
            val frontend = loaded(fileWith("CVE-2026-0001"), "frontend.vl.yaml")
            val backend = loaded(fileWith("CVE-2026-0001"), "backend.vl.yaml")

            val outcome = generate(frontend, backend).generated()

            outcome.collected.map { it.primaryId } shouldContainExactly List(2) { cve("CVE-2026-0001") }
            outcome.report.entries.map { it.primaryId } shouldContainExactly listOf(cve("CVE-2026-0001"))
            outcome.report.inputs shouldContainExactly listOf("frontend.vl.yaml", "backend.vl.yaml")
        }

        test("records the project, the time, the version and the resolved filter, and encodes the report") {
            val project = loaded(fileWith("CVE-2026-0001"))

            val outcome = generate(project, filter = FilterRequest(asOf = "1.0.0")).generated()

            outcome.report.project shouldBe ACME
            outcome.report.generatedAt shouldBe GENERATED_AT
            outcome.report.vulnlogVersion shouldBe "1.2.3-test"
            outcome.report.filter shouldBe ResolvedFilter(releases = setOf(release("1.0.0")))
            outcome.report.asOf shouldBe release("1.0.0")
            outcome.content shouldBe ImpactReportEncoder.encode(outcome.report)
        }

        test("filters each file before merging") {
            val notAffected = loaded(fileWith("CVE-2026-0001", NOT_AFFECTED))
            val affected = loaded(fileWith("CVE-2026-0002"))

            val outcome = generate(notAffected, affected, filter = FilterRequest(states = setOf("open"))).generated()

            outcome.report.entries.map { it.primaryId } shouldContainExactly listOf(cve("CVE-2026-0002"))
        }

        test("as of the deployed release, a fix that ships later still reads as open") {
            val project = loaded(fileWith("CVE-2026-0001", resolution = resolution("2.0.0")))

            val outcome = generate(project, filter = FilterRequest(asOf = "1.0.0")).generated()

            outcome.report.entries.map { it.state } shouldContainExactly listOf(WorkState.OPEN)
        }

        test("refuses files of different projects, before it looks at the filter") {
            val other = Project("Other Corp", "Other App", "Other Security Team")
            val acme = loaded(fileWith("CVE-2026-0001"))
            val otherProject = loaded(fileWith("CVE-2026-0002", project = other))

            val outcome = generate(acme, otherProject, filter = FilterRequest(reporter = "bogus"))

            outcome shouldBe ProjectsDiffer(listOf(ACME, other))
        }

        test("rejects every bad filter value at once") {
            val project = loaded(fileWith("CVE-2026-0001"))

            val outcome = generate(project, filter = FilterRequest(reporter = "bogus", states = setOf("later")))

            outcome shouldBe
                FilterRejected(
                    listOf(FilterProblem.UnknownReporter("bogus"), FilterProblem.UnknownStates(listOf("later"))),
                )
        }
    })
