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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

private val defaultProject = Project("Acme Corp", "Acme Web App", "Security Team")
private val defaultInstant: Instant = Instant.parse("2026-01-15T10:30:00Z")
private const val DEFAULT_VERSION = "1.2.3"

private fun entry(
    primaryId: VulnId = VulnId.Cve("CVE-2026-1234"),
    ids: Set<VulnId> = setOf(primaryId),
    state: WorkState = WorkState.OPEN,
    impact: Impact = Impact.Affected(Severity.HIGH),
    disposition: Disposition? = null,
    analysis: String? = "Under review",
    releases: Set<Release> = setOf(Release("1.0.0")),
    fixedIn: Set<Release> = emptySet(),
    description: String? = "RCE in example-lib",
) = ImpactEntry(
    primaryId = primaryId,
    state = state,
    ids = ids,
    shortDescription = description,
    impact = impact,
    disposition = disposition,
    analysis = analysis,
    reportFor = releases,
    fixedIn = fixedIn,
)

private fun render(
    entries: List<ImpactEntry>,
    generatedAt: Instant = defaultInstant,
    vulnlogVersion: String = DEFAULT_VERSION,
    inputs: List<String> = listOf("vulnlog.vl"),
    filter: ResolvedFilter = ResolvedFilter(),
    asOf: Release? = null,
): String =
    ImpactReportEncoder.encode(
        ImpactReport(
            project = defaultProject,
            entries = entries,
            generatedAt = generatedAt,
            vulnlogVersion = vulnlogVersion,
            inputs = inputs,
            filter = filter,
            asOf = asOf,
        ),
    )

private fun String.embeddedData(): String = substringAfter("var VULNLOG_DATA = ").substringBefore(";\n")

class ImpactReportEncoderTest :
    FunSpec({

        test("fills the template, Content-Security-Policy included, with the report data") {
            val entries = listOf(entry())

            val html = render(entries)

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "default-src 'none'"
            html shouldContain "Acme Web App"
            html shouldContain "CVE-2026-1234"
            html shouldNotContain "VULNLOG_DATA_PLACEHOLDER"
        }

        test("keeps text from ending the script element, and the text reads back unchanged") {
            val hostile = "</script><script>alert(1)</script><!-- & > \u2028 \u2029"

            val data = render(listOf(entry(analysis = hostile))).embeddedData()

            listOf("<", ">", "&", "\u2028", "\u2029").filter(data::contains).shouldBeEmpty()
            JsonMapper.shared().readTree(data)["entries"][0]["analysis"].stringValue() shouldBe hostile
        }

        test("renders a report without entries") {
            val entries = emptyList<ImpactEntry>()

            val html = render(entries)

            html shouldContain "<!DOCTYPE html>"
            html shouldContain "\"entries\":[]"
        }

        test("serializes every entry with its verdict and its detail") {
            val entries =
                listOf(
                    entry(primaryId = VulnId.Cve("CVE-2026-1234"), impact = Impact.Affected(Severity.HIGH)),
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-5678"),
                        impact = Impact.NotAffected("vulnerable code not in execute path"),
                    ),
                )

            val html = render(entries)

            html shouldContain "CVE-2026-1234"
            html shouldContain "\"verdict\":\"affected\""
            html shouldContain "\"severity\":\"high\""
            html shouldContain "CVE-2026-5678"
            html shouldContain "\"verdict\":\"not affected\""
            html shouldContain "vulnerable code not in execute path"
        }

        test("serializes aliases and fix releases") {
            val withAliasAndFixes =
                entry(
                    primaryId = VulnId.Cve("CVE-2026-1234"),
                    ids = setOf(VulnId.Cve("CVE-2026-1234"), VulnId.Ghsa("GHSA-abcd-1234-efgh")),
                    fixedIn = setOf(Release("1.1.0"), Release("2.0.1")),
                )

            val html = render(listOf(withAliasAndFixes))

            html shouldContain "GHSA-abcd-1234-efgh"
            html shouldContain "1.1.0"
            html shouldContain "2.0.1"
        }

        test("serializes the version, the inputs and the generation time") {
            val generatedAt = Instant.parse("2026-05-02T08:15:30Z")

            val html =
                render(
                    listOf(entry()),
                    generatedAt = generatedAt,
                    vulnlogVersion = "9.9.9-test",
                    inputs = listOf("project.vl", "deps.vl"),
                )

            html shouldContain "9.9.9-test"
            html shouldContain "project.vl"
            html shouldContain "deps.vl"
            html shouldContain "2026-05-02T08:15:30Z"
        }

        test("names the applied filter under its canonical tokens, in the order Vulnlog declares them") {
            val filter =
                ResolvedFilter(
                    reporter = ReporterType.CARGO_AUDIT,
                    releases = setOf(Release("1.1.0"), Release("1.2.0")),
                    tags = setOf(Tag("production"), Tag("frontend")),
                    states = setOf(WorkState.NOT_APPLICABLE, WorkState.OPEN),
                    verdicts = setOf(VerdictKind.AFFECTED),
                    dispositions = setOf(Disposition.WONT_FIX, Disposition.WILL_FIX),
                )

            val html = render(listOf(entry()), filter = filter, asOf = Release("1.2.0"))

            html shouldContain
                "\"filter\":{\"asOf\":\"1.2.0\",\"tags\":[\"frontend\",\"production\"],\"reporter\":\"cargo-audit\"," +
                "\"states\":[\"open\",\"not applicable\"],\"verdicts\":[\"affected\"]," +
                "\"dispositions\":[\"will fix\",\"wont fix\"]}"
        }

        // Retired with issue #161; the report must not bring it back as a label.
        test("renders the stated disposition, never the retired risk acceptable verdict") {
            val accepted =
                entry(
                    state = WorkState.ACCEPTED,
                    impact = Impact.Affected(Severity.HIGH),
                    disposition = Disposition.WONT_FIX,
                )

            val html = render(listOf(accepted))

            html shouldContain "wont fix"
            html shouldNotContain "risk acceptable"
        }

        test("omits the verdict while the entry is untriaged") {
            val untriaged = entry(state = WorkState.UNDER_INVESTIGATION, impact = Impact.Unknown)

            val html = render(listOf(untriaged))

            html shouldContain "\"verdict\":null"
        }

        test("sorts entries by state, then severity") {
            fun entry(
                id: String,
                state: WorkState,
                severity: Severity,
            ) = entry(primaryId = VulnId.Cve(id), state = state, impact = Impact.Affected(severity))
            val entries =
                listOf(
                    entry("CVE-2026-0001", WorkState.RESOLVED, Severity.LOW),
                    entry("CVE-2026-0002", WorkState.OPEN, Severity.MEDIUM),
                    entry("CVE-2026-0003", WorkState.OPEN, Severity.CRITICAL),
                )

            val html = render(entries)

            val positions = listOf("CVE-2026-0003", "CVE-2026-0002", "CVE-2026-0001").map(html::indexOf)
            positions shouldNotContain -1
            positions.shouldBeStrictlyIncreasing()
        }
    })
