// Copyright the Vulnlog contributors
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
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

private const val GOLDEN_RESOURCE = "/report/golden-vulnlog-report-simple.html"
private val GOLDEN_SOURCE: Path = Path.of("src/test/resources/report/golden-vulnlog-report-simple.html")

class HtmlReportGoldenTest :
    FunSpec({
        test("matches golden HTML snapshot") {
            val actual = renderHtmlReport(goldenFixture())

            if (shouldUpdateGolden()) {
                Files.createDirectories(GOLDEN_SOURCE.parent)
                Files.writeString(GOLDEN_SOURCE, actual)
                println("Golden HTML updated at $GOLDEN_SOURCE")
                return@test
            }

            val expected =
                HtmlReportGoldenTest::class.java
                    .getResourceAsStream(GOLDEN_RESOURCE)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error(
                        "Golden HTML missing at classpath $GOLDEN_RESOURCE. " +
                            "Run with UPDATE_GOLDEN=1 to create it.",
                    )

            actual shouldBe expected
        }
    })

private fun shouldUpdateGolden(): Boolean = System.getenv("UPDATE_GOLDEN") in listOf("1", "true")

private fun goldenFixture() =
    toDto(
        project = Project("Acme Corp", "Acme Web App", "Security Team"),
        entries =
            listOf(
                ReportingEntry(
                    primaryId = VulnId.Cve("CVE-2026-1111"),
                    state = WorkState.OPEN,
                    ids = setOf(VulnId.Cve("CVE-2026-1111"), VulnId.Ghsa("GHSA-aaaa-bbbb-cccc")),
                    shortDescription = "Critical RCE in example-lib",
                    impact = Impact.Affected(Severity.CRITICAL),
                    analysis = "Confirmed exploitable on the public ingress path; patch in progress.",
                    reportFor = setOf(Release("1.0.0"), Release("1.1.0")),
                    fixedIn = setOf(Release("1.2.0")),
                ),
                ReportingEntry(
                    primaryId = VulnId.Cve("CVE-2026-2222"),
                    state = WorkState.OPEN,
                    ids = setOf(VulnId.Cve("CVE-2026-2222")),
                    shortDescription = null,
                    impact = Impact.NotAffected("vulnerable code not in execute path"),
                    analysis = null,
                    reportFor = setOf(Release("1.1.0")),
                    fixedIn = emptySet(),
                ),
                ReportingEntry(
                    primaryId = VulnId.Cve("CVE-2026-3333"),
                    state = WorkState.RESOLVED,
                    ids = setOf(VulnId.Cve("CVE-2026-3333")),
                    shortDescription = "DoS via large payload",
                    impact = Impact.Affected(Severity.MEDIUM),
                    analysis = "Mitigated by request size limits at the edge.",
                    reportFor = setOf(Release("1.0.0")),
                    fixedIn = setOf(Release("1.0.1")),
                ),
            ),
        generatedAt = Instant.parse("2026-05-02T10:30:00Z"),
        vulnlogVersion = "1.2.3-test",
        inputs = listOf("frontend.vl", "backend.vl"),
        filter =
            FilterDataDto(
                release = "1.1.0",
                tags = listOf("production"),
                reporter = "trivy",
            ),
    )
