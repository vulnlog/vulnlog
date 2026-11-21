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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

private val defaultProject = Project("Acme Corp", "Acme Web App", "Security Team")
private val defaultInstant: Instant = Instant.parse("2026-01-15T10:30:00Z")
private const val DEFAULT_VERSION = "1.2.3"

private fun entry(
    primaryId: VulnId = VulnId.Cve("CVE-2026-1234"),
    aliases: Set<VulnId> = emptySet(),
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
    aliases = aliases,
    shortDescription = description,
    impact = impact,
    disposition = disposition,
    analysis = analysis,
    reportFor = releases,
    fixedIn = fixedIn,
)

private fun report(
    entries: List<ImpactEntry>,
    generatedAt: Instant = defaultInstant,
    vulnlogVersion: String = DEFAULT_VERSION,
    inputs: List<String> = listOf("vulnlog.vl"),
    filter: ResolvedFilter = ResolvedFilter(),
    asOf: Release? = null,
) = ImpactReport(
    project = defaultProject,
    entries = entries,
    generatedAt = generatedAt,
    vulnlogVersion = vulnlogVersion,
    inputs = inputs,
    filter = filter,
    asOf = asOf,
)

private fun readJson(text: String): JsonNode = JsonMapper.shared().readTree(text)

private fun ImpactReport.json(): JsonNode = readJson(ImpactReportEncoder.encodeToJson(this))

private fun JsonNode.texts(): List<String> = values().map { it.stringValue() }

private fun String.embeddedData(): String = substringAfter("var VULNLOG_DATA = ").substringBefore(";\n")

class ImpactReportEncoderTest :
    FunSpec({

        context("JSON") {

            test("names the format version, the project, the Vulnlog version, the inputs and the generation time") {
                val generatedAt = Instant.parse("2026-05-02T08:15:30Z")
                val report =
                    report(
                        listOf(entry()),
                        generatedAt = generatedAt,
                        vulnlogVersion = "9.9.9-test",
                        inputs = listOf("project.vl", "deps.vl"),
                    )

                val json = report.json()

                json["format_version"].intValue() shouldBe 1
                json["project"]["name"].stringValue() shouldBe "Acme Web App"
                json["vulnlog_version"].stringValue() shouldBe "9.9.9-test"
                json["inputs"].texts() shouldContainExactly listOf("project.vl", "deps.vl")
                json["generated_at"].stringValue() shouldBe "2026-05-02T08:15:30Z"
            }

            test("writes every entry with its verdict, severity and justification") {
                val entries =
                    listOf(
                        entry(primaryId = VulnId.Cve("CVE-2026-1234"), impact = Impact.Affected(Severity.HIGH)),
                        entry(
                            primaryId = VulnId.Cve("CVE-2026-5678"),
                            state = WorkState.NOT_APPLICABLE,
                            impact = Impact.NotAffected("vulnerable code not in execute path"),
                        ),
                    )

                val json = report(entries).json()["entries"]

                json[0]["primary_id"].stringValue() shouldBe "CVE-2026-1234"
                json[0]["verdict"].stringValue() shouldBe "affected"
                json[0]["severity"].stringValue() shouldBe "high"
                json[0]["verdict_detail"].isNull shouldBe true
                json[1]["verdict"].stringValue() shouldBe "not affected"
                json[1]["verdict_detail"].stringValue() shouldBe "vulnerable code not in execute path"
            }

            test("names each state as the Vulnlog file does") {
                val entries =
                    WorkState.entries.mapIndexed { index, state ->
                        entry(primaryId = VulnId.Cve("CVE-2026-000$index"), state = state)
                    }

                val states = report(entries).json()["entries"].values().map { it["state"].stringValue() }

                states shouldContainExactly
                    listOf("open", "under investigation", "accepted", "resolved", "not applicable")
            }

            test("writes the aliases and the fix releases") {
                val withAliasAndFixes =
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-1234"),
                        aliases = setOf(VulnId.Ghsa("GHSA-abcd-1234-efgh")),
                        fixedIn = setOf(Release("1.1.0"), Release("2.0.1")),
                    )

                val json = report(listOf(withAliasAndFixes)).json()["entries"][0]

                json["aliases"].texts() shouldContainExactly listOf("GHSA-abcd-1234-efgh")
                json["fixed_in"].texts() shouldContainExactly listOf("1.1.0", "2.0.1")
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

                val json = report(listOf(entry()), filter = filter, asOf = Release("1.2.0")).json()["filter"]

                json shouldBe
                    readJson(
                        """{"as_of":"1.2.0","tags":["frontend","production"],"reporter":"cargo-audit",""" +
                            """"states":["open","not applicable"],"verdicts":["affected"],""" +
                            """"dispositions":["will fix","wont fix"]}""",
                    )
            }

            // Retired with issue #161; the report must not bring it back as a label.
            test("writes the stated disposition, never the retired risk acceptable verdict") {
                val accepted =
                    entry(
                        state = WorkState.ACCEPTED,
                        impact = Impact.Affected(Severity.HIGH),
                        disposition = Disposition.WONT_FIX,
                    )

                val text = ImpactReportEncoder.encodeToJson(report(listOf(accepted)))

                readJson(text)["entries"][0]["disposition"].stringValue() shouldBe "wont fix"
                text shouldNotContain "risk acceptable"
            }

            test("writes a null verdict while the entry is untriaged") {
                val untriaged = entry(state = WorkState.UNDER_INVESTIGATION, impact = Impact.Unknown)

                val json = report(listOf(untriaged)).json()["entries"][0]

                json["verdict"].isNull shouldBe true
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

                val ids = report(entries).json()["entries"].values().map { it["primary_id"].stringValue() }

                ids shouldContainExactly listOf("CVE-2026-0003", "CVE-2026-0002", "CVE-2026-0001")
            }

            test("writes a report without entries") {
                val json = report(emptyList()).json()

                json["entries"].isEmpty shouldBe true
            }

            test("keeps text as written, without the escaping the HTML view needs") {
                val text = "Used only for tests & build tooling <internal>"

                val json = ImpactReportEncoder.encodeToJson(report(listOf(entry(analysis = text))))

                json shouldContain text
            }
        }

        context("HTML") {

            test("fills the template, Content-Security-Policy included, with the report data") {
                val html = ImpactReportEncoder.encodeToHtml(report(listOf(entry())))

                html shouldContain "<!DOCTYPE html>"
                html shouldContain "default-src 'none'"
                html shouldContain "Acme Web App"
                html shouldContain "CVE-2026-1234"
                html shouldNotContain "VULNLOG_DATA_PLACEHOLDER"
            }

            test("embeds the same document as the JSON output") {
                val report = report(listOf(entry(), entry(primaryId = VulnId.Cve("CVE-2026-5678"))))

                val embedded = ImpactReportEncoder.encodeToHtml(report).embeddedData()

                readJson(embedded) shouldBe report.json()
            }

            test("keeps text from ending the script element, and the text reads back unchanged") {
                val hostile = "</script><script>alert(1)</script><!-- & > \u2028 \u2029"

                val data = ImpactReportEncoder.encodeToHtml(report(listOf(entry(analysis = hostile)))).embeddedData()

                listOf("<", ">", "&", "\u2028", "\u2029").filter(data::contains).shouldBeEmpty()
                readJson(data)["entries"][0]["analysis"].stringValue() shouldBe hostile
            }
        }
    })
