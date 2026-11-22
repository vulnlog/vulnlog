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
import io.kotest.matchers.collections.shouldHaveSize
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

private fun ImpactReport.table(layout: TextLayout = TextLayout.PLAIN): String =
    ImpactReportEncoder.encodeToTable(this, layout)

private val trailingBlank = Regex(" (\u001B\\[[0-9;]*m)*$")

/** The lines of the Filter value: the labelled line and the indented lines that continue it. */
private fun filterLines(text: String): List<String> {
    val lines = text.lines()
    val start = lines.indexOfFirst { it.startsWith("Filter ") }
    return (listOf(lines[start]) + lines.drop(start + 1).takeWhile { it.startsWith(" ") && it.isNotBlank() })
        .map { it.removePrefix("Filter").trim() }
}

private val overviewLine = Regex("""^(\S.*?)\s{2,}(\S.*)$""")

/** The label and value of each overview line above the table, as the grid aligns them. */
private fun overview(text: String): Map<String, String> =
    text
        .lines()
        .takeWhile { !it.startsWith("┌") }
        .mapNotNull { overviewLine.find(it) }
        .associate { it.groupValues[1] to it.groupValues[2] }

/**
 * The lines of the table, split into the blocks the rules across the ID column divide. The rule between the facts and
 * the paragraphs of an entry starts with the ID column, so it stays in the block of its entry.
 */
private fun tableBlocks(text: String): List<List<String>> =
    text
        .lines()
        .dropWhile { !it.startsWith("┌") }
        .fold(listOf<List<String>>()) { blocks, line ->
            if (line.startsWith("│")) {
                blocks.dropLast(1) + listOf(blocks.last() + line)
            } else {
                blocks +
                    listOf(emptyList())
            }
        }.filter { it.isNotEmpty() }

/** A state heading spans the whole table and stands alone between two rules. */
private fun stateHeadings(text: String): List<String> =
    tableBlocks(text)
        .map { block -> block.map { it.removeSurrounding("│") } }
        .filter { it.size == 1 && '│' !in it.single() }
        .map { it.single().trim() }

/** The only entry block of [text]: the header block comes first and the state heading second. */
private fun singleEntry(text: String): List<String> = tableBlocks(text).drop(2).single()

private fun isFactsRule(line: String): Boolean = '├' in line

/** The trimmed cells of the fact lines of an entry block, which end at the rule above the paragraphs. */
private fun facts(entry: List<String>): List<List<String>> =
    entry.takeWhile { !isFactsRule(it) }.map { line -> line.removeSurrounding("│").split('│').map(String::trim) }

/** The primary ID and the aliases of an entry block, one piece per line of the ID column. */
private fun ids(entry: List<String>): List<String> =
    entry.map { it.drop(1).takeWhile { char -> char != '│' && char != '├' }.trim() }.filter { it.isNotEmpty() }

/** The lines of the paragraph cell beside the ID column, below the facts. */
private fun paragraphLines(entry: List<String>): List<String> =
    entry
        .dropWhile { !isFactsRule(it) }
        .drop(1)
        .map { it.substringAfter('│').substringAfter('│').removeSuffix("│") }

/** The paragraph cell of an entry block, with its paragraphs separated by blank lines. */
private fun paragraphs(entry: List<String>): String = paragraphLines(entry).joinToString("\n") { it.trim() }.trim()

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

        context("table") {

            test("names the project, the generation time, the inputs, the filter and the Vulnlog version") {
                val filter = ResolvedFilter(states = setOf(WorkState.OPEN), tags = setOf(Tag("frontend")))
                val report =
                    report(
                        listOf(entry()),
                        generatedAt = Instant.parse("2026-05-02T08:15:30Z"),
                        inputs = listOf("project.vl", "deps.vl"),
                        filter = filter,
                        asOf = Release("1.2.0"),
                    )

                val text = report.table()

                text.lines().take(2) shouldContainExactly
                    listOf("Vulnerability Impact Report: Acme Web App", "Acme Corp, Security Team")
                overview(text).filterKeys { it in setOf("Generated", "Inputs", "Filter", "Vulnlog") } shouldBe
                    mapOf(
                        "Generated" to "2026-05-02 08:15:30 UTC",
                        "Inputs" to "project.vl, deps.vl",
                        "Filter" to "as of 1.2.0; tags frontend; state open",
                        "Vulnlog" to "1.2.3",
                    )
            }

            test("says when no filter applies") {
                val text = report(listOf(entry())).table()

                overview(text)["Filter"] shouldBe "none"
            }

            test("counts the entries by state, and the open and accepted ones by severity, in report order") {
                val entries =
                    listOf(
                        entry(primaryId = VulnId.Cve("CVE-2026-0001"), state = WorkState.ACCEPTED),
                        entry(primaryId = VulnId.Cve("CVE-2026-0002"), state = WorkState.UNDER_INVESTIGATION),
                        entry(primaryId = VulnId.Cve("CVE-2026-0003"), impact = Impact.Affected(Severity.HIGH)),
                        entry(primaryId = VulnId.Cve("CVE-2026-0004"), impact = Impact.Affected(Severity.CRITICAL)),
                    )

                val counts = overview(report(entries).table())

                counts.filterKeys { it in setOf("Total", "State", "Open severity", "Accepted severity") } shouldBe
                    mapOf(
                        "Total" to "4 entries",
                        "State" to "2 open, 1 under investigation, 1 accepted",
                        "Open severity" to "1 critical, 1 high",
                        "Accepted severity" to "1 high",
                    )
            }

            test("groups the entries under one heading per state, in report order") {
                val entries =
                    listOf(
                        entry(primaryId = VulnId.Cve("CVE-2026-0001"), state = WorkState.RESOLVED),
                        entry(primaryId = VulnId.Cve("CVE-2026-0002"), state = WorkState.NOT_APPLICABLE),
                        entry(primaryId = VulnId.Cve("CVE-2026-0003")),
                        entry(primaryId = VulnId.Cve("CVE-2026-0004")),
                    )

                val headings = stateHeadings(report(entries).table())

                headings shouldContainExactly listOf("OPEN (2)", "RESOLVED (1)", "NOT APPLICABLE (1)")
            }

            test("lists the facts of an entry in columns, with the aliases under the primary ID") {
                val withAlias =
                    entry(
                        primaryId = VulnId.Cve("CVE-2026-1234"),
                        aliases = setOf(VulnId.Ghsa("GHSA-abcd-1234-efgh")),
                        disposition = Disposition.WILL_FIX,
                        releases = setOf(Release("1.0.0"), Release("1.1.0")),
                        fixedIn = setOf(Release("1.2.0")),
                    )

                val entry = singleEntry(report(listOf(withAlias)).table())

                facts(entry).first() shouldContainExactly
                    listOf("CVE-2026-1234", "high", "will fix", "1.0.0, 1.1.0", "1.2.0")
                ids(entry) shouldContainExactly listOf("CVE-2026-1234", "GHSA-abcd-1234-efgh")
            }

            test("writes the description and the analysis beside the IDs, under the facts, as paragraphs") {
                val entry =
                    singleEntry(
                        report(listOf(entry(description = "RCE in example-lib", analysis = "Reachable"))).table(),
                    )

                paragraphs(entry) shouldBe "RCE in example-lib\n\nAnalysis: Reachable"
            }

            test("gives the justification of an entry not affected under its impact, not among the paragraphs") {
                val notAffected =
                    entry(
                        state = WorkState.NOT_APPLICABLE,
                        impact = Impact.NotAffected("Vulnerable code cannot be controlled by adversary"),
                        description = "RCE in example-lib",
                        analysis = null,
                    )

                val entry = singleEntry(report(listOf(notAffected)).table())

                facts(entry).map { it[1] } shouldContainExactly
                    listOf("not affected", "Vulnerable code cannot be controlled by adversary")
                paragraphs(entry) shouldBe "RCE in example-lib"
            }

            test("wraps a long justification within the Impact column of a narrow table") {
                val justification = "Vulnerable code cannot be controlled by adversary"
                val notAffected = entry(state = WorkState.NOT_APPLICABLE, impact = Impact.NotAffected(justification))

                val text = report(listOf(notAffected)).table(TextLayout(80, colors = false))

                val impact = facts(singleEntry(text)).map { it[1] }
                text.lines().maxOf { it.length } shouldBe 80
                impact.first() shouldBe "not affected"
                impact.drop(1).joinToString(" ") shouldBe justification
            }

            test("writes only the facts of an entry without description, justification or analysis") {
                val untriaged =
                    entry(
                        state = WorkState.UNDER_INVESTIGATION,
                        impact = Impact.Unknown,
                        description = null,
                        analysis = null,
                    )

                val entry = singleEntry(report(listOf(untriaged)).table())

                entry shouldHaveSize 1
                facts(entry).single()[1] shouldBe "not triaged"
            }

            test("leaves out blank texts and the trailing line break of a block scalar") {
                val entry = singleEntry(report(listOf(entry(description = "  ", analysis = "Reachable\n"))).table())

                paragraphs(entry) shouldBe "Analysis: Reachable"
            }

            test("removes escape sequences from file text and shows other control characters as replacements") {
                val hostile = "A\u001B[2JB \u001B]0;title\u0007C \u001B[8mhidden\u001B[0m D\rE\u0008F\r\nG"
                val report = report(listOf(entry(description = hostile, analysis = null)))

                val plain = report.table()
                val colored = report.table(TextLayout(100, colors = true))

                paragraphs(singleEntry(plain)) shouldBe "AB C hidden D\uFFFDE\uFFFDF\nG"
                colored shouldNotContain "\u001B[8m"
                colored shouldNotContain "\u001B]"
            }

            test("breaks a long ID after a hyphen and keeps every column within the layout width") {
                val snyk = entry(primaryId = VulnId.Snyk("SNYK-JAVA-COMFASTERXMLJACKSONCORE-3038424"))

                val text = report(listOf(snyk)).table(TextLayout(80, colors = false))

                text.lines().maxOf { it.length } shouldBe 80
                text shouldContain "│ Fixed in "
                ids(singleEntry(text)) shouldContainExactly listOf("SNYK-JAVA-", "COMFASTERXMLJACKSONCORE-", "3038424")
            }

            test("wraps the filter only between its groups") {
                val filter =
                    ResolvedFilter(
                        reporter = ReporterType.TRIVY,
                        tags = setOf(Tag("production"), Tag("frontend")),
                        states = setOf(WorkState.OPEN, WorkState.ACCEPTED, WorkState.UNDER_INVESTIGATION),
                        verdicts = setOf(VerdictKind.AFFECTED),
                        dispositions = setOf(Disposition.WILL_FIX, Disposition.WONT_FIX),
                    )
                val groups =
                    listOf(
                        "as of 1.1.0",
                        "tags frontend, production",
                        "reporter trivy",
                        "state under investigation, open, accepted",
                        "verdict affected",
                        "disposition will fix, wont fix",
                    )

                val text =
                    report(
                        listOf(entry()),
                        filter = filter,
                        asOf = Release("1.1.0"),
                    ).table(TextLayout(80, colors = false))

                val lines = filterLines(text)
                lines.size shouldBe 3
                lines.flatMap { it.removeSuffix(";").split("; ") } shouldContainExactly groups
            }

            test("ends no entry with a blank line, also when its last line is wrapped") {
                val wrapped = entry(description = "x".repeat(60), analysis = "y".repeat(30) + " " + "z".repeat(29))

                val entry = singleEntry(report(listOf(wrapped)).table(TextLayout(80, colors = false)))

                paragraphLines(entry).last().isBlank() shouldBe false
            }

            test("fills the layout width, but not below the narrowest readable table nor above a readable line") {
                val report = report(listOf(entry()))

                val widths = listOf(100, 30, 200).map { width -> report.table(TextLayout(width, colors = false)) }

                widths.map { text -> text.lines().first { it.startsWith("┌") }.length } shouldContainExactly
                    listOf(100, 80, 120)
            }

            test("writes escape codes only when the layout allows colors") {
                val report = report(listOf(entry()))

                val plain = report.table(TextLayout(100, colors = false))
                val colored = report.table(TextLayout(100, colors = true))

                plain shouldNotContain "\u001B["
                colored shouldContain "\u001B["
            }

            test("trims the padding at the end of every line, also in front of closing style codes") {
                val report = report(listOf(entry()))

                val texts = listOf(report.table(), report.table(TextLayout(100, colors = true)))

                texts.flatMap { it.lines() }.filter { trailingBlank.containsMatchIn(it) }.shouldBeEmpty()
            }

            test("ends the table with a line break, as a text file does") {
                val texts = listOf(report(listOf(entry())).table(), report(emptyList()).table())

                texts.map { it.takeLast(2) } shouldContainExactly listOf("┘\n", ".\n")
            }

            test("says that there are no entries, without an empty table") {
                val text = report(emptyList()).table()

                overview(text)["Total"] shouldBe "0 entries"
                text shouldContain "No vulnerability entries to display."
                text shouldNotContain "┌"
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
