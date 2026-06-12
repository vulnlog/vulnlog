// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.result.FormatRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain

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
        val mapper = createYamlMapper()

        fun check(content: String) = checkFormat(VulnlogFileRaw(content), ParseValidationVersion.V1, mapper)

        val canonical = formatYaml(VulnlogFileRaw(BASE_INPUT), mapper).content

        context("drift invariants") {

            test("byte-canonical documents produce zero findings") {
                listOf(BASE_INPUT, MULTILINE_INPUT)
                    .map { formatYaml(VulnlogFileRaw(it), mapper).content }
                    .forEach { canonicalContent ->
                        check(canonicalContent).shouldBeEmpty()
                    }
            }

            test("any non-canonical document produces at least one finding (catch-all)") {
                // quoting deviation: no named rule covers it, the layout catch-all must fire
                val quoted = canonical.replace("verdict: not affected", "verdict: \"not affected\"")

                val findings = check(quoted)

                findings.shouldNotBeEmpty()
                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_LAYOUT)
            }
        }

        context("rules") {

            // BASE_INPUT carries no header, so its canonical form has none either
            val canonicalWithHeader = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n$canonical"

            test("a document without the optional schema header is canonical") {
                canonical shouldNotContain "# \$schema:"
                check(canonical).shouldBeEmpty()
            }

            test("a document with the optional schema header is canonical") {
                check(canonicalWithHeader).shouldBeEmpty()
            }

            test("a missing document-start marker is reported") {
                // drop the '---' marker from the header-less canonical form
                val withoutStart = canonical.lines().drop(1).joinToString("\n")

                val findings = check(withoutStart)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_DOCUMENT_START)
                findings.first().path shouldBe "line 1"
                findings.first().message shouldContain "'---'"
            }

            test("a present schema header with a non-canonical URL is reported") {
                val wrongUrl =
                    canonicalWithHeader.replace(
                        "https://vulnlog.dev/schema/vulnlog-v1.json",
                        "https://example.com/other.json",
                    )

                val findings = check(wrongUrl)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_DOCUMENT_START)
                findings.first().message shouldContain "# \$schema:"
            }

            test("single-element array in block style") {
                val blockArray =
                    canonical.replace(
                        "    releases: [1.0.0]",
                        "    releases:\n      - 1.0.0",
                    )

                val findings = check(blockArray)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_ARRAY_STYLE)
                findings.first().path shouldBe "vulnerabilities[CVE-2026-0001].releases"
                findings.first().message shouldContain "flow array"
                findings.first().message shouldMatch Regex("Line \\d+:.*")
            }

            test("multi-element array in flow style") {
                val flowTags =
                    canonical.replace(
                        "    packages: [\"pkg:npm/example-lib@2.3.0\"]",
                        "    packages: [\"pkg:npm/example-lib@2.3.0\", \"pkg:npm/other-lib@1.0.0\"]",
                    )

                val findings = check(flowTags)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_ARRAY_STYLE)
                findings.first().message shouldContain "block list"
            }

            test("entry fields out of canonical order") {
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

            test("long prose written as a plain scalar") {
                // the folded block's wrap points are emitter-determined, so replace the whole block
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
                findings.first().message shouldContain "folded block"
            }

            test("comments are reported as not preserved") {
                val commented = canonical.replace("vulnerabilities:", "# audit note\nvulnerabilities:")

                val findings = check(commented)

                findings.map { it.rule } shouldBe listOf(FormatRule.COMMENTS_NOT_PRESERVED)
                findings.first().message shouldContain "YAML comments are removed on write."
            }

            test("blank-line deviation falls into the layout catch-all") {
                val collapsed = canonical.replace("\n\nreleases:", "\nreleases:")

                val findings = check(collapsed)

                findings.map { it.rule } shouldBe listOf(FormatRule.NON_CANONICAL_LAYOUT)
            }
        }

        context("rendering") {

            test("a finding with a path renders as rule id, path and message") {
                val blockArray = canonical.replace("    releases: [1.0.0]", "    releases:\n      - 1.0.0")

                val rendered = renderFormatFinding(check(blockArray).first())

                rendered shouldMatch
                    Regex("\\[non-canonical-array-style] vulnerabilities\\[CVE-2026-0001]\\.releases: Line \\d+:.*")
            }

            test("a file-level finding renders without a path") {
                val commented = canonical.replace("vulnerabilities:", "# audit note\nvulnerabilities:")

                val rendered = renderFormatFinding(check(commented).first())

                rendered shouldMatch Regex("\\[comments-not-preserved] [^:].*")
            }
        }
    })
