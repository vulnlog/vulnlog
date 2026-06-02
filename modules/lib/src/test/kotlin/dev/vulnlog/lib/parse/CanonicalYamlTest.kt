// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

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

        test("long prose renders as a folded block scalar") {
            val long = "The vulnerable code path is not reachable in our application because we are safe."
            val yaml = CanonicalYaml.renderEntry(entry(analysis = long), mapper)

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
    })
