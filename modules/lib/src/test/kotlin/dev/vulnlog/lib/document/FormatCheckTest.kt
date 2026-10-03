// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.finding.FormatFinding
import dev.vulnlog.lib.finding.FormatRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch

private val BASE_INPUT =
    """
    schemaVersion: "1"
    project:
      organization: Acme
      name: App
      author: Sec
    releases:
      - id: 1.0.0
        published_at: 2026-01-01
    vulnerabilities:
      - id: CVE-2026-0001
        description: Remote code execution in example-lib
        releases: [1.0.0]
        packages: ["pkg:npm/example-lib@2.3.0"]
        reports:
          - reporter: trivy
        analysis: >-
          The vulnerable code path is not reachable in our application because we only
          use the safe subset of the API, and the affected module is excluded from all
          production builds of every supported release.
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent() + "\n"

private val MULTILINE_INPUT =
    """
    schemaVersion: "1"
    project:
      organization: Acme
      name: App
      author: Sec
    releases:
      - id: 1.0.0
    vulnerabilities:
      - id: CVE-2026-0002
        releases: [1.0.0]
        packages: []
        reports: []
        analysis: |-
          Affected paths:
          - decode()
          - encode()
    """.trimIndent() + "\n"

class FormatCheckTest :
    FunSpec({

        fun check(content: String) = checkFormat(parsed(content))

        // BASE_INPUT has no schema header, so neither has its canonical form.
        val canonical = formatYaml(parsed(BASE_INPUT))
        val canonicalWithHeader = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n$canonical"
        val blockArray = canonical.replace("    releases: [1.0.0]", "    releases:\n      - 1.0.0")
        val commented = canonical.replace("vulnerabilities:", "# audit note\nvulnerabilities:")

        context("drift invariants") {

            test("a canonical document yields no finding, with or without the schema header") {
                val documents = listOf(canonical, canonicalWithHeader, formatYaml(parsed(MULTILINE_INPUT)))

                val findings = documents.map(::check)

                findings shouldBe List(3) { emptyList() }
            }

            test("a deviation no rule names yields the layout catch-all and nothing else") {
                val quoted = canonical.replace("verdict: not affected", "verdict: \"not affected\"")
                val collapsed = canonical.replace("\n\nreleases:", "\nreleases:")

                val rules = listOf(quoted, collapsed).map { check(it).map(FormatFinding::rule) }

                rules shouldBe List(2) { listOf(FormatRule.NON_CANONICAL_LAYOUT) }
            }

            test("every deviation a rule names is reported, and the catch-all stays silent") {
                val commentedBlockArray = blockArray.replace("vulnerabilities:", "# audit note\nvulnerabilities:")

                val findings = check(commentedBlockArray)

                findings.map { it.rule } shouldBe
                    listOf(FormatRule.NON_CANONICAL_ARRAY_STYLE, FormatRule.COMMENTS_NOT_PRESERVED)
            }
        }

        context("rules") {

            test("a missing document-start marker is reported") {
                val withoutStart = canonical.lines().drop(1).joinToString("\n")

                val findings = check(withoutStart)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_DOCUMENT_START)
                findings.first().path shouldBe "line 1"
                findings.first().message shouldBe "File should start with '---'."
            }

            test("a present schema header with a non-canonical URL is reported") {
                val wrongUrl =
                    canonicalWithHeader.replace(
                        "https://vulnlog.dev/schema/vulnlog-v1.json",
                        "https://example.com/other.json",
                    )

                val findings = check(wrongUrl)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_DOCUMENT_START)
                findings.first().message shouldContain "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json"
            }

            test("a single-element array in block style is reported") {
                val findings = check(blockArray)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_ARRAY_STYLE)
                findings.first().path shouldBe "vulnerabilities[CVE-2026-0001].releases"
                findings.first().message shouldMatch Regex("Line \\d+: .*flow array.*")
            }

            test("a multi-element array in flow style is reported") {
                val flowPackages =
                    canonical.replace(
                        "    packages: [\"pkg:npm/example-lib@2.3.0\"]",
                        "    packages: [\"pkg:npm/example-lib@2.3.0\", \"pkg:npm/other-lib@1.0.0\"]",
                    )

                val findings = check(flowPackages)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_ARRAY_STYLE)
                findings.first().message shouldContain "block list"
            }

            test("entry fields out of canonical order are reported") {
                val swapped =
                    canonical.replace(
                        "    description: Remote code execution in example-lib\n    releases: [1.0.0]",
                        "    releases: [1.0.0]\n    description: Remote code execution in example-lib",
                    )

                val findings = check(swapped)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_FIELD_ORDER)
                findings.first().path shouldBe "vulnerabilities[CVE-2026-0001]"
                findings.first().message shouldContain "'releases' is misplaced"
            }

            test("long prose written as a plain scalar is reported") {
                // The emitter decides where the folded block wraps, so the whole block is replaced.
                val plainAnalysis =
                    Regex("    analysis: >-\\n(?:      .*\\n)+").replace(
                        canonical,
                        "    analysis: The vulnerable code path is not reachable in our application " +
                            "because we only use the safe subset of the API, and the affected module " +
                            "is excluded from all production builds of every supported release.\n",
                    )

                val findings = check(plainAnalysis)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_BLOCK_SCALAR)
                findings.first().path shouldBe "vulnerabilities[CVE-2026-0001].analysis"
                findings.first().message shouldContain "is a folded block (>-) (found plain)"
            }

            test("a short value written as a block scalar is reported") {
                val foldedDescription =
                    canonical.replace(
                        "    description: Remote code execution in example-lib",
                        "    description: >-\n      Remote code execution in example-lib",
                    )

                val findings = check(foldedDescription)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_BLOCK_SCALAR)
                findings.first().path shouldBe "vulnerabilities[CVE-2026-0001].description"
                findings.first().message shouldContain "is plain (found a folded block (>-))"
            }

            test("comments are reported as not preserved") {
                val findings = check(commented)

                findings.map { it.rule } shouldBe listOf(FormatRule.COMMENTS_NOT_PRESERVED)
                findings.first().message shouldBe "YAML comments are removed on write."
            }
        }
    })
