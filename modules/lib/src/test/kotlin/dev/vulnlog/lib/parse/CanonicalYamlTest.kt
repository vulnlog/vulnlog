// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.ResolutionDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle

class CanonicalYamlTest :
    FunSpec({
        val mapper = createYamlMapper()

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

        test("single-element scalar list renders as a flow array, type-safe unquoted") {
            val yaml = CanonicalYaml.renderEntry(entry(releases = listOf("0.11.0")), mapper)

            yaml shouldContain "releases: [0.11.0]"
        }

        test("colon-bearing scalars (purls) are double-quoted in flow arrays") {
            val yaml = CanonicalYaml.renderEntry(entry(packages = listOf("pkg:maven/org.example/lib@1.2.3")), mapper)

            yaml shouldContain """packages: ["pkg:maven/org.example/lib@1.2.3"]"""
        }

        test("multi-element list renders as a block array") {
            val yaml = CanonicalYaml.renderEntry(entry(releases = listOf("0.11.0", "0.12.0")), mapper)

            yaml shouldContain "releases:\n  - 0.11.0\n  - 0.12.0"
        }

        test("a single report (list of mappings) stays in block style, not inline flow") {
            val yaml = CanonicalYaml.renderEntry(entry(reports = listOf(ReportEntryDto(reporter = "trivy"))), mapper)

            yaml shouldContain "reports:\n  - reporter: trivy"
            yaml shouldNotContain "[{"
        }

        test("absent report date is omitted, not rendered as null") {
            val yaml = CanonicalYaml.renderEntry(entry(reports = listOf(ReportEntryDto(reporter = "trivy"))), mapper)

            yaml shouldNotContain "at: null"
        }

        test("absent resolution date is omitted, not rendered as null") {
            val pendingFix = entry().copy(resolution = ResolutionDto(release = "0.12.0", note = "pending"))
            val yaml = CanonicalYaml.renderEntry(pendingFix, mapper)

            yaml shouldContain "in: 0.12.0"
            yaml shouldNotContain "at: null"
        }

        test("prose longer than two line widths renders as a folded block scalar") {
            val long = "The vulnerable code path is not reachable in our application because we are safe. ".repeat(2)
            val yaml = CanonicalYaml.renderEntry(entry(analysis = long.trim()), mapper)

            yaml shouldContain "analysis: >"
        }

        test("enums and ids stay unquoted") {
            val yaml =
                CanonicalYaml.renderEntry(
                    entry(verdict = "not affected", justification = "vulnerable code not in execute path"),
                    mapper,
                )

            yaml shouldContain "id: CVE-2026-0001"
            yaml shouldContain "verdict: not affected"
            yaml shouldContain "justification: vulnerable code not in execute path"
        }

        test("field order follows DTO declaration order") {
            val yaml = CanonicalYaml.renderEntry(entry(description = "short summary"), mapper)
            val keys = yaml.lines().filter { it.matches(Regex("""^\w+:.*""")) }.map { it.substringBefore(':') }

            keys shouldBe listOf("id", "description", "releases", "packages", "reports")
        }

        test("a multi-line string renders as a literal block scalar") {
            val multiLine = "Affected paths:\n  - parser.decode()\nNone are reachable."
            val yaml = CanonicalYaml.renderEntry(entry(analysis = multiLine), mapper)

            yaml shouldContain "analysis: |-"
            yaml shouldContain "  - parser.decode()"
            yaml shouldNotContain "analysis: >"
        }

        test("a very long string without newlines defaults to a folded block scalar") {
            val long = "The vulnerable code path is not reachable in our application because we are safe. ".repeat(2)
            val yaml = CanonicalYaml.renderEntry(entry(analysis = long.trim()), mapper)

            yaml shouldContain "analysis: >"
        }

        test("a moderately long spaced string stays plain instead of folding") {
            val wouldWrap = "Time-of-check Time-of-use (TOCTOU) Race Condition (CWE-367) in the rsync daemon."
            val yaml = CanonicalYaml.renderEntry(entry(description = wouldWrap), mapper)

            yaml shouldContain "description: Time-of-check"
            yaml shouldNotContain "description: >"
        }

        test("a short spaced string stays an inline plain scalar") {
            val yaml = CanonicalYaml.renderEntry(entry(description = "Remote code execution in example-lib"), mapper)

            yaml shouldContain "description: Remote code execution in example-lib"
            yaml shouldNotContain "description: >"
        }

        test("surrounding whitespace is trimmed, so a trailing space no longer forces double-quoting") {
            val trailingSpace =
                "Integer overflow vulnerability (CWE-125, CWE-190) in rsync allowing to disclose process memory. "
            val yaml = CanonicalYaml.renderEntry(entry(description = trailingSpace), mapper)

            yaml shouldContain "description: Integer overflow"
            yaml shouldNotContain "memory. \""
        }

        test("leading and trailing whitespace is stripped from a short value") {
            val yaml = CanonicalYaml.renderEntry(entry(description = "  padded  "), mapper)

            yaml shouldContain "description: padded\n"
            yaml shouldNotContain "padded  "
        }

        context("decision functions") {

            test("canonicalScalarStyle is a pure function of the value") {
                CanonicalYaml.canonicalScalarStyle("a\nb") shouldBe ScalarStyle.LITERAL
                CanonicalYaml.canonicalScalarStyle("word ".repeat(40)) shouldBe ScalarStyle.FOLDED
                CanonicalYaml.canonicalScalarStyle("word ".repeat(20)) shouldBe ScalarStyle.PLAIN
                CanonicalYaml.canonicalScalarStyle("1") shouldBe ScalarStyle.DOUBLE_QUOTED
                CanonicalYaml.canonicalScalarStyle("pkg:npm/x@1") shouldBe ScalarStyle.DOUBLE_QUOTED
                CanonicalYaml.canonicalScalarStyle("not affected") shouldBe ScalarStyle.PLAIN
            }

            test("canonicalFlowStyle puts at most one scalar element in flow style") {
                CanonicalYaml.canonicalFlowStyle(0, scalarItemsOnly = true) shouldBe FlowStyle.FLOW
                CanonicalYaml.canonicalFlowStyle(1, scalarItemsOnly = true) shouldBe FlowStyle.FLOW
                CanonicalYaml.canonicalFlowStyle(2, scalarItemsOnly = true) shouldBe FlowStyle.BLOCK
                CanonicalYaml.canonicalFlowStyle(1, scalarItemsOnly = false) shouldBe FlowStyle.BLOCK
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
                        analyzedAt = java.time.LocalDate.EPOCH,
                        severity = "s",
                        resolution = ResolutionDto(release = "0"),
                        comment = "c",
                    )
                val keys =
                    CanonicalYaml
                        .renderEntry(full, mapper)
                        .lines()
                        .filter { it.matches(Regex("""^\w+:.*""")) }
                        .map { it.substringBefore(':') }

                keys shouldBe CanonicalYaml.canonicalEntryFieldOrder(mapper)
            }
        }
    })
