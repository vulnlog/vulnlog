// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

private const val GOLDEN_DIR = "/report"
private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/report")

/** Pins the bytes of every view of one report. The JSON is an API, so a changed golden JSON is a changed contract. */
class ImpactReportGoldenTest :
    FunSpec({
        test("matches golden HTML snapshot") {
            val actual = ImpactReportEncoder.encodeToHtml(goldenFixture())

            actual shouldBe golden("vulnlog-report-simple.html", actual)
        }

        test("matches golden JSON snapshot") {
            val actual = ImpactReportEncoder.encodeToJson(goldenFixture())

            actual shouldBe golden("impact-report.json", actual)
        }

        test("matches golden table snapshot") {
            val actual = ImpactReportEncoder.encodeToTable(goldenFixture(), TextLayout.PLAIN)

            actual shouldBe golden("impact-report-table.txt", actual)
        }
    })

/** With UPDATE_GOLDEN set, writes [actual] as the new golden file and returns it, so the test passes. */
private fun golden(
    name: String,
    actual: String,
): String {
    val fileName = "golden-$name"
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(GOLDEN_SOURCE_DIR.resolve(fileName), actual)
        println("Golden file updated at ${GOLDEN_SOURCE_DIR.resolve(fileName)}")
        return actual
    }
    return ImpactReportGoldenTest::class.java
        .getResourceAsStream("$GOLDEN_DIR/$fileName")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath $GOLDEN_DIR/$fileName. Run with UPDATE_GOLDEN=1 to create it.")
}

private fun goldenFixture() =
    ImpactReport(
        project = Project("Acme Corp", "Acme Web App", "Security Team"),
        entries =
            listOf(
                ImpactEntry(
                    primaryId = VulnId.Cve("CVE-2026-1111"),
                    state = WorkState.OPEN,
                    aliases = setOf(VulnId.Ghsa("GHSA-aaaa-bbbb-cccc")),
                    shortDescription = "Critical RCE in example-lib",
                    impact = Impact.Affected(Severity.CRITICAL),
                    disposition = Disposition.WILL_FIX,
                    analysis = "Confirmed exploitable on the public ingress path; patch in progress.",
                    reportFor = setOf(Release("1.0.0"), Release("1.1.0")),
                    fixedIn = setOf(Release("1.2.0")),
                ),
                ImpactEntry(
                    primaryId = VulnId.Cve("CVE-2026-2222"),
                    state = WorkState.NOT_APPLICABLE,
                    aliases = emptySet(),
                    shortDescription = null,
                    impact = Impact.NotAffected("vulnerable code not in execute path"),
                    disposition = null,
                    analysis = null,
                    reportFor = setOf(Release("1.1.0")),
                    fixedIn = emptySet(),
                ),
                ImpactEntry(
                    primaryId = VulnId.Cve("CVE-2026-3333"),
                    state = WorkState.RESOLVED,
                    aliases = emptySet(),
                    shortDescription = "DoS via large payload",
                    impact = Impact.Affected(Severity.MEDIUM),
                    disposition = null,
                    analysis = "Mitigated by request size limits at the edge.",
                    reportFor = setOf(Release("1.0.0")),
                    fixedIn = setOf(Release("1.0.1")),
                ),
                ImpactEntry(
                    primaryId = VulnId.Cve("CVE-2026-4444"),
                    state = WorkState.ACCEPTED,
                    aliases = emptySet(),
                    shortDescription = "Prototype pollution in a build-time helper",
                    impact = Impact.Affected(Severity.HIGH),
                    disposition = Disposition.WONT_FIX,
                    analysis = "Build-time only; no upstream fix and no runtime exposure.",
                    reportFor = setOf(Release("1.1.0")),
                    fixedIn = emptySet(),
                ),
                ImpactEntry(
                    primaryId = VulnId.Cve("CVE-2026-5555"),
                    state = WorkState.UNDER_INVESTIGATION,
                    aliases = emptySet(),
                    shortDescription = "Unbounded recursion in the XML parser",
                    impact = Impact.Unknown,
                    disposition = null,
                    analysis = null,
                    reportFor = setOf(Release("1.1.0")),
                    fixedIn = emptySet(),
                ),
            ),
        generatedAt = Instant.parse("2026-05-02T10:30:00Z"),
        vulnlogVersion = "1.2.3-test",
        inputs = listOf("frontend.vl", "backend.vl"),
        filter =
            ResolvedFilter(
                reporter = ReporterType.TRIVY,
                releases = setOf(Release("1.0.0"), Release("1.1.0")),
                tags = setOf(Tag("production")),
                states = setOf(WorkState.OPEN, WorkState.ACCEPTED),
                verdicts = setOf(VerdictKind.AFFECTED),
                dispositions = setOf(Disposition.WONT_FIX),
            ),
        asOf = Release("1.1.0"),
    )
