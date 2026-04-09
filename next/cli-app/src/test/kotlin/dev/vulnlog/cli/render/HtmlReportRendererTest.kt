package dev.vulnlog.cli.render

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.report.Impact
import dev.vulnlog.cli.model.report.ReportingEntry
import dev.vulnlog.cli.model.report.WorkState
import dev.vulnlog.cli.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.cli.parse.reporting.HtmlReportWriter.renderHtmlReport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

private val defaultProject = Project("Acme Corp", "Acme Web App", "Security Team")
private val defaultDate = LocalDate.of(2026, 1, 15)

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

class HtmlReportRendererTest :
    FunSpec({

        test("renders valid HTML with project name") {
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = listOf(entry()))

            val html = renderHtmlReport(data)

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "Acme Web App"
        }

        test("contains serialized entry data") {
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = listOf(entry()))

            val html = renderHtmlReport(data)

            html shouldContain "CVE-2026-1234"
            html shouldContain "affected"
            html shouldContain "high"
        }

        test("placeholder is replaced") {
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = listOf(entry()))

            val html = renderHtmlReport(data)

            html shouldNotContain "VULNLOG_DATA_PLACEHOLDER"
        }

        test("renders with empty entries") {
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = emptyList())

            val html = renderHtmlReport(data)

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "\"entries\":[]"
        }

        test("renders multiple entries") {
            val entries =
                listOf(
                    entry(primaryId = VulnId.Cve("CVE-2026-1234")),
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-5678"),
                        impact = Impact.NotAffected("vulnerable_code_not_in_execute_path"),
                        state = WorkState.OPEN,
                    ),
                )
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = entries)

            val html = renderHtmlReport(data)

            html shouldContain "CVE-2026-1234"
            html shouldContain "CVE-2026-5678"
            html shouldContain "not_affected"
        }

        test("includes aliases in serialized data") {
            val e =
                entry(
                    primaryId = VulnId.Cve("CVE-2026-1234"),
                    ids = setOf(VulnId.Cve("CVE-2026-1234"), VulnId.Ghsa("GHSA-abcd-1234-efgh")),
                )
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = listOf(e))

            val html = renderHtmlReport(data)

            html shouldContain "GHSA-abcd-1234-efgh"
        }

        test("includes fix releases in serialized data") {
            val e =
                entry(fixedIn = setOf(Release("1.1.0"), Release("2.0.1")))
            val data = toDto(defaultProject, generatedAt = defaultDate, entries = listOf(e))

            val html = renderHtmlReport(data)

            html shouldContain "1.1.0"
            html shouldContain "2.0.1"
        }
    })
