// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.render

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.report.Impact
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.model.report.WorkState
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.lib.parse.reporting.dto.FilterDataDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.Instant

private val defaultProject = Project("Acme Corp", "Acme Web App", "Security Team")
private val defaultInstant: Instant = Instant.parse("2026-01-15T10:30:00Z")
private const val DEFAULT_VERSION = "1.2.3"
private val emptyFilter = FilterDataDto(release = null, tags = emptyList(), reporter = null)

private fun entry(
    primaryId: VulnId = VulnId.Cve("CVE-2026-1234"),
    ids: Set<VulnId> = setOf(primaryId),
    state: WorkState = WorkState.OPEN,
    impact: Impact = Impact.Affected(Severity.HIGH),
    analysis: String? = "Under review",
    releases: Set<Release> = setOf(Release("1.0.0")),
    fixedIn: Set<Release> = emptySet(),
    description: String? = "RCE in example-lib",
) = ReportingEntry(
    primaryId = primaryId,
    state = state,
    ids = ids,
    shortDescription = description,
    impact = impact,
    analysis = analysis,
    reportFor = releases,
    fixedIn = fixedIn,
)

private fun render(
    entries: List<ReportingEntry>,
    generatedAt: Instant = defaultInstant,
    vulnlogVersion: String = DEFAULT_VERSION,
    inputs: List<String> = listOf("vulnlog.vl"),
    filter: FilterDataDto = emptyFilter,
): String =
    renderHtmlReport(
        toDto(
            project = defaultProject,
            entries = entries,
            generatedAt = generatedAt,
            vulnlogVersion = vulnlogVersion,
            inputs = inputs,
            filter = filter,
        ),
    )

class HtmlReportRendererTest :
    FunSpec({

        test("renders valid HTML with project name") {
            val html = render(listOf(entry()))

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "Acme Web App"
        }

        test("contains serialized entry data") {
            val html = render(listOf(entry()))

            html shouldContain "CVE-2026-1234"
            html shouldContain "affected"
            html shouldContain "high"
        }

        test("placeholder is replaced") {
            val html = render(listOf(entry()))

            html shouldNotContain "VULNLOG_DATA_PLACEHOLDER"
        }

        test("renders with empty entries") {
            val html = render(emptyList())

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "\"entries\":[]"
        }

        test("renders multiple entries") {
            val entries =
                listOf(
                    entry(primaryId = VulnId.Cve("CVE-2026-1234")),
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-5678"),
                        impact = Impact.NotAffected("vulnerable code not in execute path"),
                        state = WorkState.OPEN,
                    ),
                )
            val html = render(entries)

            html shouldContain "CVE-2026-1234"
            html shouldContain "CVE-2026-5678"
            html shouldContain "not affected"
        }

        test("includes aliases in serialized data") {
            val e =
                entry(
                    primaryId = VulnId.Cve("CVE-2026-1234"),
                    ids = setOf(VulnId.Cve("CVE-2026-1234"), VulnId.Ghsa("GHSA-abcd-1234-efgh")),
                )
            val html = render(listOf(e))

            html shouldContain "GHSA-abcd-1234-efgh"
        }

        test("includes fix releases in serialized data") {
            val e = entry(fixedIn = setOf(Release("1.1.0"), Release("2.0.1")))
            val html = render(listOf(e))

            html shouldContain "1.1.0"
            html shouldContain "2.0.1"
        }

        test("includes vulnlog version") {
            val html = render(listOf(entry()), vulnlogVersion = "9.9.9-test")

            html shouldContain "9.9.9-test"
        }

        test("includes input file names") {
            val html = render(listOf(entry()), inputs = listOf("project.vl", "deps.vl"))

            html shouldContain "project.vl"
            html shouldContain "deps.vl"
        }

        test("includes applied filter") {
            val html =
                render(
                    listOf(entry()),
                    filter =
                        FilterDataDto(
                            release = "1.2.0",
                            tags = listOf("frontend", "production"),
                            reporter = "trivy",
                        ),
                )

            html shouldContain "1.2.0"
            html shouldContain "frontend"
            html shouldContain "trivy"
        }

        test("includes verdictDetail for not-affected entries") {
            val e =
                entry(
                    impact = Impact.NotAffected("vulnerable code not in execute path"),
                )
            val html = render(listOf(e))

            html shouldContain "vulnerable code not in execute path"
        }

        test("renders generatedAt as ISO instant") {
            val html = render(listOf(entry()), generatedAt = Instant.parse("2026-05-02T08:15:30Z"))

            html shouldContain "2026-05-02T08:15:30Z"
        }

        test("includes Content-Security-Policy meta tag") {
            val html = render(listOf(entry()))

            html shouldContain "Content-Security-Policy"
            html shouldContain "default-src 'none'"
        }

        test("sorts entries by state then severity") {
            val entries =
                listOf(
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-0001"),
                        state = WorkState.RESOLVED,
                        impact = Impact.Affected(Severity.LOW),
                    ),
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-0002"),
                        state = WorkState.OPEN,
                        impact = Impact.Affected(Severity.MEDIUM),
                    ),
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-0003"),
                        state = WorkState.OPEN,
                        impact = Impact.Affected(Severity.CRITICAL),
                    ),
                )
            val html = render(entries)

            // Critical-open should appear before medium-open, both before low-resolved.
            val criticalIdx = html.indexOf("CVE-2026-0003")
            val mediumIdx = html.indexOf("CVE-2026-0002")
            val resolvedIdx = html.indexOf("CVE-2026-0001")
            check(criticalIdx in 0..<mediumIdx) {
                "Expected CVE-2026-0003 (open critical) before CVE-2026-0002 (open medium)"
            }
            check(mediumIdx in 0..<resolvedIdx) {
                "Expected CVE-2026-0002 (open) before CVE-2026-0001 (resolved)"
            }
        }
    })
