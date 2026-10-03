// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.suppression.SuppressionEncoder
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.document.validated
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Suppression
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 4, 3)
private val SNYK_ID = VulnId.Snyk("SNYK-JAVA-001")

private fun report(
    reporter: ReporterType,
    vararg vulnIds: VulnId,
    expiresAt: LocalDate? = null,
) = ReportEntry(
    reporter = reporter,
    source = "manual review".takeIf { reporter == ReporterType.OTHER },
    vulnIds = vulnIds.toSet(),
    suppress = Suppression(expiresAt),
)

private fun entry(
    id: String,
    vararg reports: ReportEntry,
    resolution: Resolution? = null,
): VulnerabilityEntry =
    vulnerability(
        id = cve(id),
        releases = listOf(release("1.0.0")),
        reports = reports.toList(),
        verdict = Verdict.Affected(Severity.HIGH),
        resolution = resolution,
        analysis = "not reachable",
    )

private fun fileWith(vararg entries: VulnerabilityEntry): VulnlogFile =
    vulnlogFile(releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")), vulnerabilities = entries.toList())

/** Through YAML and the load step, as a driver hands it over: only the load step builds a project. */
private fun generate(
    file: VulnlogFile,
    filter: FilterRequest = FilterRequest(),
    format: SuppressionFormatRequest = SuppressionFormatRequest.Auto,
    singleFile: Boolean = false,
): SuppressionOutcome =
    generateSuppressions(validated(YamlWriter.write(file)), SuppressionRequest(filter, format, TODAY, singleFile))

private fun SuppressionOutcome.files(): List<SuppressionFile> = shouldBeInstanceOf<SuppressionOutcome.Generated>().files

class GenerateSuppressionsTest :
    FunSpec({

        test("writes one encoded file per reporter the input names") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), report(ReporterType.SNYK, SNYK_ID)))

            val files = generate(file).files()

            files.map { it.list } shouldContainExactly
                listOf(
                    SuppressionList(
                        SuppressionFormat.Trivy,
                        setOf(SuppressionEntry(cve("CVE-2026-0001"), null, "not reachable")),
                    ),
                    SuppressionList(SuppressionFormat.Snyk, setOf(SuppressionEntry(SNYK_ID, null, "not reachable"))),
                )
            files.map { it.fileName } shouldContainExactly listOf(".trivyignore.yaml", ".snyk")
            files.map { it.content } shouldContainExactly files.map { SuppressionEncoder.encode(it.list) }
        }

        test("a reporter with nothing left to suppress still gets an empty file") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), resolution = resolution("1.0.0")))

            val files = generate(file).files()

            files.map { it.list } shouldContainExactly listOf(SuppressionList(SuppressionFormat.Trivy, emptySet()))
        }

        test("reports what collecting and building left out") {
            val file =
                fileWith(
                    entry("CVE-2026-0001", report(ReporterType.TRIVY), resolution = resolution("1.0.0")),
                    entry("CVE-2026-0002", report(ReporterType.SNYK)),
                    entry("CVE-2026-0003", report(ReporterType.TRIVY, expiresAt = TODAY.minusDays(1))),
                )

            val outcome = generate(file).shouldBeInstanceOf<SuppressionOutcome.Generated>()

            outcome.collection.exclusions shouldContainExactly
                listOf(
                    SuppressionExclusion.Resolved(cve("CVE-2026-0001")),
                    SuppressionExclusion.Expired(cve("CVE-2026-0003"), ReporterType.TRIVY, TODAY.minusDays(1)),
                    SuppressionExclusion.UnsupportedIdType(cve("CVE-2026-0002"), SuppressionFormat.Snyk),
                )
        }

        test("the reporter filter narrows the files to that reporter") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), report(ReporterType.SNYK, SNYK_ID)))

            val files = generate(file, FilterRequest(reporter = "trivy")).files()

            files.map { it.fileName } shouldContainExactly listOf(".trivyignore.yaml")
        }

        test("the generic format replaces a reporter's native one") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY)))

            val files = generate(file, format = SuppressionFormatRequest.Generic).files()

            files.map { it.fileName } shouldContainExactly listOf("trivy.generic.json")
        }

        test("as of the deployed release, a fix that ships later stays suppressed") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), resolution = resolution("2.0.0")))

            val files = generate(file, FilterRequest(asOf = "1.0.0")).files()

            files.single().list.entries shouldBe setOf(SuppressionEntry(cve("CVE-2026-0001"), null, "not reachable"))
        }

        test("rejects every bad filter value at once") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY)))

            val outcome = generate(file, FilterRequest(reporter = "bogus", asOf = "9.9.9"))

            outcome shouldBe
                FilterRejected(
                    listOf(
                        FilterProblem.UnknownReporter("bogus"),
                        FilterProblem.UnknownRelease(release("9.9.9"), listOf(release("1.0.0"), release("2.0.0"))),
                    ),
                )
        }

        test("a single file fails when several reporters get a file") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), report(ReporterType.SNYK, SNYK_ID)))

            val outcome = generate(file, singleFile = true).shouldBeInstanceOf<SuppressionOutcome.SeveralReporters>()

            outcome.reporters shouldContainExactly listOf(ReporterType.TRIVY, ReporterType.SNYK)
        }

        test("a single file takes the one reporter left by the filter") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.TRIVY), report(ReporterType.SNYK, SNYK_ID)))

            val files = generate(file, FilterRequest(reporter = "snyk"), singleFile = true).files()

            files.map { it.fileName } shouldContainExactly listOf(".snyk")
        }

        test("nothing to suppress when no reporter in scope has a format") {
            val file = fileWith(entry("CVE-2026-0001", report(ReporterType.OTHER)))

            val outcome = generate(file).shouldBeInstanceOf<SuppressionOutcome.NothingToSuppress>()

            outcome.collection.exclusions shouldContainExactly
                listOf(SuppressionExclusion.UnsupportedReporter(cve("CVE-2026-0001"), ReporterType.OTHER))
        }
    })
