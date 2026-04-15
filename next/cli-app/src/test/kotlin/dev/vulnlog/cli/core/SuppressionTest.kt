// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Purl
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReportEntry
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Resolution
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.Suppression
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VexJustification
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.model.suppress.SuppressedVulnerability
import dev.vulnlog.cli.model.suppress.SuppressionOutput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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

private fun trivyReport(
    vulnIds: Set<VulnId> = emptySet(),
    suppress: Suppression? = Suppression(),
) = ReportEntry(
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

private fun suppressedVuln(
    id: VulnId = VulnId.Cve("CVE-2024-0001"),
    reporter: ReporterType = ReporterType.TRIVY,
    expiresAt: LocalDate? = null,
    analysis: String = "not affected",
) = SuppressedVulnerability(
    id = id,
    releases = listOf(releaseV1),
    reporter = reporter,
    expiresAt = expiresAt,
    analysis = analysis,
)

class SuppressionTest :
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

            test("includes affected vulnerability without resolution when suppress present") {
                val vuln = vulnerability(verdict = Verdict.Affected(Severity.HIGH))
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
            }

            test("excludes suppression expired before today") {
                val report = trivyReport(suppress = Suppression(expiresAt = today.minusDays(1)))
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result.shouldBeEmpty()
            }

            test("includes suppression expiring after today") {
                val report = trivyReport(suppress = Suppression(expiresAt = today.plusDays(30)))
                val vuln = vulnerability(reports = listOf(report), verdict = Verdict.UnderInvestigation)
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result =
                    collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
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

        context("mapToSuppression to Trivy") {

            test("maps trivy suppressions to TrivySuppression output") {
                val entry = suppressedVuln()
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.TRIVY), input)

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.TrivySuppression>()
            }

            test("filters out non-suppressable reporters") {
                val entry = suppressedVuln(reporter = ReporterType.OTHER)
                val input = mapOf(ReporterType.OTHER to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.OTHER), input)

                result.shouldBeEmpty()
            }

            test("produces correct trivy entries from CVE") {
                val entry = suppressedVuln(id = VulnId.Cve("CVE-2024-1234"), analysis = "false positive")
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.TRIVY), input)
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries shouldHaveSize 1
                trivy.entries.first().id shouldBe VulnId.Cve("CVE-2024-1234")
                trivy.entries.first().reason shouldBe "false positive"
            }

            test("produces empty TrivySuppression when no suppressions match") {
                val result = mapToSuppression(setOf(ReporterType.TRIVY), emptyMap())

                result shouldHaveSize 1
                val trivy = result.first() as SuppressionOutput.TrivySuppression
                trivy.entries.shouldBeEmpty()
            }

            test("ignores target reporters not in suppressable set") {
                val result = mapToSuppression(setOf(ReporterType.OTHER, ReporterType.TRIVY), emptyMap())

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.TrivySuppression>()
            }

            test("produces empty output when no target reporters") {
                val result = mapToSuppression(emptySet(), emptyMap())

                result.shouldBeEmpty()
            }

            test("propagates expiresAt to trivy entries") {
                val expiresAt = LocalDate.of(2026, 12, 31)
                val entry = suppressedVuln(expiresAt = expiresAt)
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.TRIVY), input)
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries.first().expiresAt shouldBe expiresAt
            }

            test("sets expiresAt to null for permanent suppression") {
                val entry = suppressedVuln(expiresAt = null)
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.TRIVY), input)
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries.first().expiresAt shouldBe null
            }

            test("deduplicates entries across multiple vulnerabilities") {
                val entry1 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"))
                val entry2 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"))
                val input = mapOf(ReporterType.TRIVY to listOf(entry1, entry2))

                val result = mapToSuppression(setOf(ReporterType.TRIVY), input)
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries shouldHaveSize 1
            }
        }

        context("mapToSuppression for Snyk") {

            test("maps snyk suppressions to SnykSuppression output") {
                val entry = suppressedVuln(id = VulnId.Snyk("SNYK-JAVA-001"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.SNYK), input)

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.SnykSuppression>()
            }

            test("produces correct snyk entries from Snyk vulnId") {
                val entry =
                    suppressedVuln(
                        id = VulnId.Snyk("SNYK-JAVA-001"),
                        reporter = ReporterType.SNYK,
                        analysis = "not exploitable",
                    )
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.SNYK), input)
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries shouldHaveSize 1
                snyk.entries.first().id shouldBe VulnId.Snyk("SNYK-JAVA-001")
                snyk.entries.first().reason shouldBe "not exploitable"
            }

            test("filters out non-snyk vuln ids for Snyk output") {
                val cveEntry = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(cveEntry))

                val result = mapToSuppression(setOf(ReporterType.SNYK), input)
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries.shouldBeEmpty()
            }

            test("produces empty SnykSuppression when no suppressions match") {
                val result = mapToSuppression(setOf(ReporterType.SNYK), emptyMap())

                result shouldHaveSize 1
                val snyk = result.first() as SuppressionOutput.SnykSuppression
                snyk.entries.shouldBeEmpty()
            }

            test("propagates expiresAt to snyk entries") {
                val expiresAt = LocalDate.of(2026, 12, 31)
                val entry =
                    suppressedVuln(
                        id = VulnId.Snyk("SNYK-JAVA-001"),
                        reporter = ReporterType.SNYK,
                        expiresAt = expiresAt,
                    )
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = mapToSuppression(setOf(ReporterType.SNYK), input)
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries.first().expiresAt shouldBe expiresAt
            }
        }

        context("mapToSuppression for multiple reporters") {

            test("produces both trivy and snyk output") {
                val result = mapToSuppression(setOf(ReporterType.TRIVY, ReporterType.SNYK), emptyMap())

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

            test("not affected with resolution is still included") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                        resolution = Resolution(release = releaseV1),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

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

            test("risk acceptable with resolution is included when suppress block present") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.RiskAcceptable(Severity.MEDIUM),
                        resolution = Resolution(release = releaseV1),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
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

            test("not affected ignores suppress expiration") {
                val report = trivyReport(suppress = Suppression(expiresAt = today.minusDays(30)))
                val vuln =
                    vulnerability(
                        reports = listOf(report),
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    )
                val file = emptyFile().copy(vulnerabilities = listOf(vuln))

                val result = collectSuppressedVulnerabilities(file, SuppressionFilter(today = today))

                result shouldHaveSize 1
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
