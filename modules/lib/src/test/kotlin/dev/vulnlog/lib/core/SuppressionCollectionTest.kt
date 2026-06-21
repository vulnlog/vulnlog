// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Suppression
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val today = LocalDate.of(2026, 4, 3)
private val defaultSchema = SchemaVersion(1, 0)
private val releaseV1 = Release("v1.0")
private val releaseV2 = Release("v2.0")

private fun emptyFile() =
    VulnlogFile(
        schemaVersion = defaultSchema,
        project = Project("org", "project", "author"),
        releases = emptyList(),
        vulnerabilities = emptyList(),
    )

private fun report(
    reporter: ReporterType,
    vulnIds: Set<VulnId> = emptySet(),
    suppress: Suppression? = Suppression(),
) = ReportEntry(
    reporter = reporter,
    vulnIds = vulnIds,
    suppress = suppress,
)

private fun trivyReport(
    vulnIds: Set<VulnId> = emptySet(),
    suppress: Suppression? = Suppression(),
) = report(
    reporter = ReporterType.TRIVY,
    vulnIds = vulnIds,
    suppress = suppress,
)

private fun vulnerability(
    id: VulnId = VulnId.Cve("CVE-2024-0001"),
    releases: List<Release> = listOf(releaseV1),
    reports: List<ReportEntry> = listOf(trivyReport()),
    tags: List<Tag> = emptyList(),
    analysis: String = "not affected",
    verdict: Verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
    resolution: Resolution? = null,
) = VulnerabilityEntry(
    id = id,
    releases = releases,
    packages = listOf(Purl.Maven("pkg:maven/com.example/lib@1.0")),
    reports = reports,
    tags = tags,
    analysis = analysis,
    verdict = verdict,
    resolution = resolution,
)

class SuppressionCollectionTest :
    FunSpec({

        context("collectSuppressedVulnerabilities") {

            test("collects vulnerability with suppressed report") {
                val file = emptyFile().copy(vulnerabilities = listOf(vulnerability()))
                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.TRIVY]!!.first().id shouldBe VulnId.Cve("CVE-2024-0001")
            }

            test("excludes reports without suppress for non-not affected verdict") {
                val report = trivyReport(suppress = null)
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))
                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("collect only report specific vulnerability ID if one specified") {
                val vuln =
                    vulnerability(
                        id = VulnId.Cve("CVE-2024-0001"),
                        releases = listOf(releaseV1),
                        reports = listOf(trivyReport(vulnIds = setOf(VulnId.Cve("CVE-2024-0002")))),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.TRIVY]!![0].id shouldBe VulnId.Cve("CVE-2024-0002")
            }

            test("collect only report specific vulnerability ID if multiple specified") {
                val vuln =
                    vulnerability(
                        id = VulnId.Cve("CVE-2024-0001"),
                        releases = listOf(releaseV1),
                        reports =
                            listOf(
                                trivyReport(
                                    vulnIds =
                                        setOf(
                                            VulnId.Cve("CVE-2024-0002"),
                                            VulnId.Cve("CVE-2024-0003"),
                                        ),
                                ),
                            ),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result[ReporterType.TRIVY]!! shouldHaveSize 2
                result[ReporterType.TRIVY]!![0].id shouldBe VulnId.Cve("CVE-2024-0002")
                result[ReporterType.TRIVY]!![1].id shouldBe VulnId.Cve("CVE-2024-0003")
            }

            test("filters by single release") {
                val vuln1 = vulnerability(id = VulnId.Cve("CVE-2024-0001"), releases = listOf(releaseV1))
                val vuln2 = vulnerability(id = VulnId.Cve("CVE-2024-0002"), releases = listOf(releaseV2))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln1, vuln2))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.TRIVY]!!.first().id shouldBe VulnId.Cve("CVE-2024-0001")
            }

            test("filters by multiple releases includes vulnerabilities for any of them") {
                val releaseV3 = Release("v3.0")
                val vuln1 = vulnerability(id = VulnId.Cve("CVE-2024-0001"), releases = listOf(releaseV1))
                val vuln2 = vulnerability(id = VulnId.Cve("CVE-2024-0002"), releases = listOf(releaseV2))
                val vuln3 = vulnerability(id = VulnId.Cve("CVE-2024-0003"), releases = listOf(releaseV3))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln1, vuln2, vuln3))

                val result =
                    collectSuppressedVulnerabilities(
                        file,
                        SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1, releaseV2)), today),
                    )

                result[ReporterType.TRIVY]!! shouldHaveSize 2
            }

            test("filters by tag") {
                val tag = Tag("backend")
                val vuln1 = vulnerability(id = VulnId.Cve("CVE-2024-0001"), tags = listOf(tag))
                val vuln2 = vulnerability(id = VulnId.Cve("CVE-2024-0002"), tags = emptyList())
                val file = emptyFile().copy(vulnerabilities = listOf(vuln1, vuln2))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(VulnlogFilter(tags = setOf(tag)), today))

                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.TRIVY]!!.first().id shouldBe VulnId.Cve("CVE-2024-0001")
            }

            test("excludes affected vulnerability with resolution") {
                val resolution = Resolution(release = releaseV1)
                val vuln =
                    vulnerability(
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = resolution,
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("includes affected vulnerability whose resolution targets a release outside the filter") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1),
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.TRIVY]!!.first().id shouldBe VulnId.Cve("CVE-2024-0001")
            }

            test("excludes affected vulnerability whose resolution shipped within the filter releases") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1, releaseV2),
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1, releaseV2)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result.shouldBeEmpty()
            }

            test("includes affected vulnerability without resolution when suppress present") {
                val vuln = vulnerability(verdict = Verdict.Affected(Severity.HIGH))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            ReporterType.entries.forEach { reporter ->
                test("excludes suppression expired before today for $reporter") {
                    val report = report(reporter = reporter, suppress = Suppression(expiresAt = today.minusDays(1)))
                    val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                    val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                    val result =
                        collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                    result.shouldBeEmpty()
                }
            }

            ReporterType.entries.forEach { reporter ->
                test("includes suppression expiring after today for $reporter") {
                    val report = report(reporter = reporter, suppress = Suppression(expiresAt = today.plusDays(30)))
                    val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                    val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                    val result =
                        collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                    result shouldHaveSize 1
                }
            }

            test("filters by reporter type") {
                val trivyReport = trivyReport()
                val snykReport = ReportEntry(reporter = ReporterType.SNYK, suppress = Suppression())
                val vuln = vulnerability(reports = listOf(trivyReport, snykReport))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(
                        file,
                        SuppressionFilter(VulnlogFilter(reporter = ReporterType.TRIVY), today),
                    )

                result shouldHaveSize 1
                result.keys.first() shouldBe ReporterType.TRIVY
            }

            test("groups by reporter type") {
                val trivyReport = trivyReport()
                val snykReport = ReportEntry(reporter = ReporterType.SNYK, suppress = Suppression())
                val vuln = vulnerability(reports = listOf(trivyReport, snykReport))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 2
            }
        }

        context("verdict-based suppression eligibility") {

            test("not affected is always included without suppress block") {
                val report = trivyReport(suppress = null)
                val vuln =
                    vulnerability(
                        reports = listOf(report),
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("not affected with resolution is excluded") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                        resolution = Resolution(release = releaseV1),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("not affected with resolution outside the filter releases is still included") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val filter = SuppressionFilter(VulnlogFilter(releases = setOf(releaseV1)), today)
                val result = collectSuppressedVulnerabilities(file, filter)

                result shouldHaveSize 1
            }

            test("affected with resolution is excluded regardless of suppress block") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV1),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("risk acceptable with resolution is excluded regardless of suppress block") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.RiskAcceptable(Severity.MEDIUM),
                        resolution = Resolution(release = releaseV1),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("affected without resolution is included when suppress block present") {
                val vuln = vulnerability(verdict = Verdict.Affected(Severity.HIGH))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("affected without resolution is excluded when suppress block absent") {
                val report = trivyReport(suppress = null)
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.Affected(Severity.HIGH))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("risk acceptable is included when suppress block present") {
                val vuln = vulnerability(verdict = Verdict.RiskAcceptable(Severity.MEDIUM))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("risk acceptable is excluded when suppress block absent") {
                val report = trivyReport(suppress = null)
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.RiskAcceptable(Severity.MEDIUM))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("under_investigation is included when suppress block present") {
                val vuln = vulnerability(verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("under_investigation is excluded when suppress block absent") {
                val report = trivyReport(suppress = null)
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("risk acceptable respects suppress expiration") {
                val report = trivyReport(suppress = Suppression(expiresAt = today.minusDays(30)))
                val vuln =
                    vulnerability(
                        reports = listOf(report),
                        verdict = Verdict.RiskAcceptable(Severity.MEDIUM),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("expires_at equal to today is included") {
                val report = trivyReport(suppress = Suppression(expiresAt = today))
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("not affected respects suppress expiration") {
                val report = trivyReport(suppress = Suppression(expiresAt = today.minusDays(30)))
                val vuln =
                    vulnerability(
                        reports = listOf(report),
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 0
            }
        }

        context("collectSuppressedVulnerabilities with no filters") {

            test("returns empty map for file with no vulnerabilities") {
                val result = collectSuppressedVulnerabilities(emptyFile(), SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("excludes affected vulnerability with resolution even if no date") {
                val resolution = Resolution(release = releaseV1, at = null)
                val file =
                    emptyFile().copy(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    verdict = Verdict.Affected(Severity.HIGH),
                                    resolution = resolution,
                                ),
                            ),
                    )

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("includes suppression with no expiresAt") {
                val report = trivyReport(suppress = Suppression(expiresAt = null))
                val file =
                    emptyFile().copy(
                        vulnerabilities = listOf(vulnerability(reports = listOf(report))),
                    )

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("collects multiple reports from same vulnerability") {
                val report1 = trivyReport()
                val report2 = ReportEntry(reporter = ReporterType.SNYK, suppress = Suppression())
                val file =
                    emptyFile().copy(
                        vulnerabilities = listOf(vulnerability(reports = listOf(report1, report2))),
                    )

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 2
                result[ReporterType.TRIVY]!! shouldHaveSize 1
                result[ReporterType.SNYK]!! shouldHaveSize 1
            }
        }
    })
