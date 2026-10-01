// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Suppression
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.suppress.SuppressionCollectionResult
import dev.vulnlog.lib.model.suppress.SuppressionExclusion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 4, 3)
private val V1 = release("v1.0")
private val V2 = release("v2.0")
private val NOT_AFFECTED = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH)
private val AFFECTED = Verdict.Affected(Severity.HIGH)
private val WONT_FIX = Verdict.Affected(Severity.MEDIUM, Disposition.WONT_FIX)

private fun report(
    reporter: ReporterType = ReporterType.TRIVY,
    vulnIds: Set<VulnId> = emptySet(),
    suppress: Suppression? = Suppression(),
) = ReportEntry(reporter = reporter, vulnIds = vulnIds, suppress = suppress)

private fun entry(
    id: String,
    releases: List<Release> = listOf(V1),
    reports: List<ReportEntry> = listOf(report()),
    tags: List<Tag> = emptyList(),
    verdict: Verdict = NOT_AFFECTED,
    resolution: Resolution? = null,
): VulnerabilityEntry =
    vulnerability(
        id = cve(id),
        releases = releases,
        reports = reports,
        tags = tags,
        verdict = verdict,
        resolution = resolution,
    )

private fun collect(
    vararg entries: VulnerabilityEntry,
    filter: ResolvedFilter = ResolvedFilter(),
): SuppressionCollectionResult =
    collectSuppressedVulnerabilities(vulnlogFile(vulnerabilities = entries.toList()), SuppressionFilter(filter, TODAY))

private fun SuppressionCollectionResult.includedIds(): List<VulnId> = included.values.flatten().map { it.id }

class SuppressionCollectionTest :
    FunSpec({

        context("eligibility") {

            test("a report with a suppress block is suppressed, whatever the verdict") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", verdict = NOT_AFFECTED),
                        entry("CVE-2024-0002", verdict = AFFECTED),
                        entry("CVE-2024-0003", verdict = WONT_FIX),
                        entry("CVE-2024-0004", verdict = Verdict.UnderInvestigation),
                    )

                val result = collect(*entries)

                result.includedIds() shouldContainExactly
                    listOf(cve("CVE-2024-0001"), cve("CVE-2024-0002"), cve("CVE-2024-0003"), cve("CVE-2024-0004"))
                result.exclusions.shouldBeEmpty()
            }

            test("a report without a suppress block is suppressed only for a not affected verdict") {
                val unsuppressed = listOf(report(suppress = null))
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", reports = unsuppressed, verdict = NOT_AFFECTED),
                        entry("CVE-2024-0002", reports = unsuppressed, verdict = AFFECTED),
                        entry("CVE-2024-0003", reports = unsuppressed, verdict = WONT_FIX),
                        entry("CVE-2024-0004", reports = unsuppressed, verdict = Verdict.UnderInvestigation),
                    )

                val result = collect(*entries)

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0001"))
            }
        }

        context("resolution") {

            test("a resolution in scope excludes the entry and reports it as resolved") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", verdict = AFFECTED, resolution = Resolution(release = V1)),
                        entry("CVE-2024-0002", verdict = NOT_AFFECTED, resolution = Resolution(release = V1)),
                    )

                val result = collect(*entries)

                result.included shouldBe emptyMap()
                result.exclusions shouldContainExactly
                    listOf(
                        SuppressionExclusion.ResolvedVulnerability(cve("CVE-2024-0001")),
                        SuppressionExclusion.ResolvedVulnerability(cve("CVE-2024-0002")),
                    )
            }

            test("a resolution in a release outside the filter keeps the entry") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", verdict = AFFECTED, resolution = Resolution(release = V2)),
                        entry("CVE-2024-0002", verdict = NOT_AFFECTED, resolution = Resolution(release = V2)),
                    )

                val result = collect(*entries, filter = ResolvedFilter(releases = setOf(V1)))

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0001"), cve("CVE-2024-0002"))
            }

            test("a resolution shipped in one of the filter releases excludes the entry") {
                val resolved =
                    entry(
                        "CVE-2024-0001",
                        releases = listOf(V1, V2),
                        verdict = AFFECTED,
                        resolution = Resolution(release = V2),
                    )

                val result = collect(resolved, filter = ResolvedFilter(releases = setOf(V1, V2)))

                result.included shouldBe emptyMap()
            }
        }

        context("expiry") {

            test("a suppression is active up to and including its expiry date") {
                val expiresOn = { date: LocalDate -> listOf(report(suppress = Suppression(expiresAt = date))) }
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", reports = expiresOn(TODAY.minusDays(1)), verdict = WONT_FIX),
                        entry("CVE-2024-0002", reports = expiresOn(TODAY), verdict = Verdict.UnderInvestigation),
                        entry("CVE-2024-0003", reports = expiresOn(TODAY.plusDays(30)), verdict = AFFECTED),
                    )

                val result = collect(*entries)

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0002"), cve("CVE-2024-0003"))
                result.exclusions shouldContainExactly
                    listOf(
                        SuppressionExclusion.ExpiredSuppression(
                            cve("CVE-2024-0001"),
                            ReporterType.TRIVY,
                            TODAY.minusDays(1),
                        ),
                    )
            }

            test("an expired suppression ends a not affected entry too") {
                val expired = listOf(report(suppress = Suppression(expiresAt = TODAY.minusDays(30))))

                val result = collect(entry("CVE-2024-0001", reports = expired, verdict = NOT_AFFECTED))

                result.included shouldBe emptyMap()
            }
        }

        context("filters") {

            test("keeps the entries of any requested release") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", releases = listOf(V1)),
                        entry("CVE-2024-0002", releases = listOf(V2)),
                        entry("CVE-2024-0003", releases = listOf(release("v3.0"))),
                    )

                val result = collect(*entries, filter = ResolvedFilter(releases = setOf(V1, V2)))

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0001"), cve("CVE-2024-0002"))
            }

            test("keeps the entries carrying a requested tag") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", tags = listOf(tag("backend"))),
                        entry("CVE-2024-0002"),
                    )

                val result = collect(*entries, filter = ResolvedFilter(tags = setOf(tag("backend"))))

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0001"))
            }

            test("keeps the reports of the requested reporter") {
                val bothReporters = entry("CVE-2024-0001", reports = listOf(report(), report(ReporterType.SNYK)))

                val result = collect(bothReporters, filter = ResolvedFilter(reporter = ReporterType.TRIVY))

                result.included.keys shouldBe setOf(ReporterType.TRIVY)
            }

            test("reports neither inclusions nor exclusions for entries outside the filter") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001"),
                        entry("CVE-2024-0002", verdict = AFFECTED, resolution = Resolution(release = V1)),
                    )

                val result = collect(*entries, filter = ResolvedFilter(tags = setOf(tag("backend"))))

                result.included shouldBe emptyMap()
                result.exclusions.shouldBeEmpty()
            }

            test("reports a resolved entry only for the requested reporter") {
                val entries =
                    arrayOf(
                        entry("CVE-2024-0001", verdict = AFFECTED, resolution = Resolution(release = V1)),
                        entry(
                            "CVE-2024-0002",
                            reports = listOf(report(ReporterType.SNYK)),
                            verdict = AFFECTED,
                            resolution = Resolution(release = V1),
                        ),
                    )

                val result = collect(*entries, filter = ResolvedFilter(reporter = ReporterType.TRIVY))

                result.exclusions shouldContainExactly
                    listOf(SuppressionExclusion.ResolvedVulnerability(cve("CVE-2024-0001")))
            }
        }

        context("grouping") {

            test("groups the suppressions by reporter") {
                val bothReporters = entry("CVE-2024-0001", reports = listOf(report(), report(ReporterType.SNYK)))

                val result = collect(bothReporters)

                result.included.mapValues { (_, suppressions) -> suppressions.map { it.id } } shouldBe
                    mapOf(
                        ReporterType.TRIVY to listOf(cve("CVE-2024-0001")),
                        ReporterType.SNYK to listOf(cve("CVE-2024-0001")),
                    )
            }

            test("suppresses the ids a report names instead of the entry's own id") {
                val namingReport = report(vulnIds = setOf(cve("CVE-2024-0002"), cve("CVE-2024-0003")))

                val result = collect(entry("CVE-2024-0001", reports = listOf(namingReport)))

                result.includedIds() shouldContainExactly listOf(cve("CVE-2024-0002"), cve("CVE-2024-0003"))
            }
        }
    })
