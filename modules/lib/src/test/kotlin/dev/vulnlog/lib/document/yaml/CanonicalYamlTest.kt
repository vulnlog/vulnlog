// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import dev.vulnlog.lib.document.dto.ReportEntryDto
import dev.vulnlog.lib.document.dto.ResolutionDto
import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import java.time.LocalDate

class CanonicalYamlTest :
    FunSpec({

        fun entry(
            releases: List<String> = listOf("0.11.0"),
            packages: List<String> = listOf("pkg:maven/org.example/lib@1.2.3"),
            reports: List<ReportEntryDto> = listOf(ReportEntryDto(reporter = "trivy")),
            description: String? = null,
            analysis: String? = null,
            verdict: String? = null,
            justification: String? = null,
        ) = VulnerabilityEntryDto(
            id = "CVE-2026-0001",
            description = description,
            releases = releases,
            packages = packages,
            reports = reports,
            analysis = analysis,
            verdict = verdict,
            justification = justification,
        )

        test("a single scalar element renders as a flow array, double-quoted only when it bears a colon") {
            val single = entry(releases = listOf("0.11.0"), packages = listOf("pkg:maven/org.example/lib@1.2.3"))

            val yaml = CanonicalYaml.renderEntry(single)

            yaml shouldContain "releases: [0.11.0]"
            yaml shouldContain """packages: ["pkg:maven/org.example/lib@1.2.3"]"""
        }

        test("several elements or a list of mappings render as a block list") {
            val several =
                entry(releases = listOf("0.11.0", "0.12.0"), reports = listOf(ReportEntryDto(reporter = "trivy")))

            val yaml = CanonicalYaml.renderEntry(several)

            yaml shouldContain "releases:\n  - 0.11.0\n  - 0.12.0"
            yaml shouldContain "reports:\n  - reporter: trivy"
        }

        test("absent values are omitted, not rendered as null") {
            val pendingFix = entry().copy(resolution = ResolutionDto(release = "0.12.0", note = "pending"))

            val yaml = CanonicalYaml.renderEntry(pendingFix)

            yaml shouldContain "in: 0.12.0"
            yaml shouldNotContain "null"
        }

        test("enums and ids stay unquoted") {
            val notAffected = entry(verdict = "not affected", justification = "vulnerable code not in execute path")

            val yaml = CanonicalYaml.renderEntry(notAffected)

            yaml shouldContain "id: CVE-2026-0001"
            yaml shouldContain "verdict: not affected"
            yaml shouldContain "justification: vulnerable code not in execute path"
        }

        test("a multi-line string renders as a literal block scalar") {
            val multiLine = "Affected paths:\n  - parser.decode()\nNone are reachable."

            val yaml = CanonicalYaml.renderEntry(entry(analysis = multiLine))

            yaml shouldContain "analysis: |-\n"
            yaml shouldContain "  - parser.decode()"
        }

        test("a spaced string above the fold threshold renders as a folded block scalar") {
            val long = "The vulnerable code path is not reachable in our application because we are safe. ".repeat(2)

            val yaml = CanonicalYaml.renderEntry(entry(analysis = long.trim()))

            yaml shouldContain "analysis: >"
        }

        test("a spaced string below the fold threshold stays plain on one line, beyond the emitter's default width") {
            val wouldWrap = "Time-of-check Time-of-use (TOCTOU) Race Condition (CWE-367) in the rsync daemon."

            val yaml = CanonicalYaml.renderEntry(entry(description = wouldWrap))

            yaml shouldContain "description: $wouldWrap\n"
        }

        test("surrounding whitespace is trimmed rather than forcing double quotes") {
            val padded = "  padded  "

            val yaml = CanonicalYaml.renderEntry(entry(description = padded))

            yaml shouldContain "description: padded\n"
        }

        test("renderEntryListItem indents the entry as one list item and leaves out empty lists") {
            val minimal = entry().copy(aliases = emptyList(), tags = emptyList())

            val yaml = CanonicalYaml.renderEntryListItem(minimal)

            yaml shouldContain "  - id: CVE-2026-0001\n    releases:"
            yaml shouldNotContain "---"
            yaml shouldNotContain "aliases:"
            yaml shouldNotContain "tags:"
        }

        context("decision functions") {

            test("canonicalScalarStyle picks the style by the value alone") {
                val values = listOf("a\nb", "word ".repeat(40), "word ".repeat(20), "1", "pkg:npm/x@1", "not affected")

                val styles = values.map(CanonicalYaml::canonicalScalarStyle)

                styles shouldBe
                    listOf(
                        ScalarStyle.LITERAL,
                        ScalarStyle.FOLDED,
                        ScalarStyle.PLAIN,
                        ScalarStyle.DOUBLE_QUOTED,
                        ScalarStyle.DOUBLE_QUOTED,
                        ScalarStyle.PLAIN,
                    )
            }

            test("canonicalFlowStyle puts at most one scalar element in flow style") {
                val shapes = listOf(0 to true, 1 to true, 2 to true, 1 to false)

                val styles = shapes.map { (count, scalarOnly) -> CanonicalYaml.canonicalFlowStyle(count, scalarOnly) }

                styles shouldBe listOf(FlowStyle.FLOW, FlowStyle.FLOW, FlowStyle.BLOCK, FlowStyle.BLOCK)
            }

            test("renderEntry key order matches canonicalEntryFieldOrder") {
                val full =
                    entry(
                        description = "d",
                        analysis = "a",
                        verdict = "v",
                        justification = "j",
                    ).copy(
                        name = "n",
                        aliases = listOf("GHSA-0000-0000-0000"),
                        tags = listOf("t"),
                        analyzedAt = LocalDate.EPOCH,
                        severity = "s",
                        resolution = ResolutionDto(release = "0"),
                        comment = "c",
                    )

                val keys =
                    CanonicalYaml
                        .renderEntry(full)
                        .lines()
                        .filter { it.matches(Regex("""^\w+:.*""")) }
                        .map { it.substringBefore(':') }

                keys shouldBe CanonicalYaml.canonicalEntryFieldOrder()
            }
        }
    })
