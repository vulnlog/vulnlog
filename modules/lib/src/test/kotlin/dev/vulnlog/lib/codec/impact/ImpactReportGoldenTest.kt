// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.readOrUpdateGolden
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/** Pins the bytes of every view of one report. The JSON is an API, so a changed golden JSON is a changed contract. */
class ImpactReportGoldenTest :
    FunSpec({
        test("matches golden HTML snapshot") {
            val actual = ImpactReportEncoder.encodeToHtml(goldenFixture())

            actual shouldBe readOrUpdateGolden("golden-vulnlog-report-simple.html", actual)
        }

        test("matches golden JSON snapshot") {
            val actual = ImpactReportEncoder.encodeToJson(goldenFixture())

            actual shouldBe readOrUpdateGolden("golden-impact-report.json", actual)
        }

        test("matches golden table snapshot") {
            val actual = ImpactReportEncoder.encodeToTable(goldenFixture(), TextLayout.PLAIN)

            actual shouldBe readOrUpdateGolden("golden-impact-report-table.txt", actual)
        }
    })

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
                    impact = Impact.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
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
