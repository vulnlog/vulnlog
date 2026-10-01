// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

// Block sequences at the key's indent are valid YAML; an earlier formatter duplicated their entries.
private val COLUMN0_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: Acme
      name: App
      author: Sec

    tags:
    - id: foo
      description: bar

    releases:
    - id: 1.0.0
      published_at: 2026-01-01
    - id: 2.0.0

    vulnerabilities: []
    """.trimIndent() + "\n"

private val QUOTED_ENTRIES_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: Acme
      name: App
      author: Sec

    releases:
      - id: "1.0.0"
        published_at: "2026-01-01"

    vulnerabilities:

      - id: "CVE-2026-0001"
        releases:
          - "1.0.0"
        packages:
          - "pkg:npm/example-lib@2.3.0"
        reports:
          - reporter: "trivy"
        verdict: "not affected"
        justification: "vulnerable code not in execute path"

      - id: "CVE-2026-0002"
        releases:
          - "1.0.0"
        packages:
          - "pkg:npm/other-lib@1.0.0"
        reports:
          - reporter: "grype"
        verdict: "not affected"
        justification: "vulnerable code not in execute path"
    """.trimIndent() + "\n"

private val STYLED_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
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
        releases: [1.0.0]
        packages: ["pkg:npm/example-lib@2.3.0"]
        reports:
          - reporter: trivy
        analysis: |
          Affected paths:
            - parser.decode()
          None are reachable from our entry points.
        comment: >
          This is a sufficiently long folded comment that should stay folded after
          formatting because it is longer than two line widths, which is the
          threshold beyond which prose becomes a folded block instead of a plain
          wrapped scalar.
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent() + "\n"

private val COMMENTED_YAML =
    """
    # ${'$'}schema: https://example.com/custom.json
    schemaVersion: "1"

    project:
      organization: Acme
      name: App
      author: Sec

    # reviewed by the security team
    releases:
      - id: 1.0.0
        published_at: 2026-01-01

    vulnerabilities:

      - id: CVE-2026-0001
        # temporary, recheck after upgrade
        releases: [1.0.0]
        packages: ["pkg:npm/example-lib@2.3.0"]
        reports:
          - reporter: trivy
    """.trimIndent() + "\n"

private val FLOW_YAML =
    """
    schemaVersion: "1"
    project: {organization: Acme, name: App, author: Sec}
    releases: [{id: 1.0.0, published_at: 2026-01-01}]
    vulnerabilities: [{id: CVE-2026-0001, releases: [1.0.0],
      packages: ["pkg:npm/example-lib@2.3.0"], reports: [{reporter: trivy}]}]
    """.trimIndent() + "\n"

private val CANONICAL_LEGACY_VERDICT_YAML =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme
      name: App
      author: Sec

    releases:
      - id: 1.0.0

    vulnerabilities:

      - id: CVE-2026-0001
        releases: [1.0.0]
        packages: ["pkg:npm/example-lib@2.3.0"]
        reports:
          - reporter: trivy
        verdict: risk acceptable
        severity: low
    """.trimIndent() + "\n"

private fun occurrences(
    haystack: String,
    needle: String,
) = Regex(Regex.escape(needle)).findAll(haystack).count()

class FormatTest :
    FunSpec({

        test("indents column-0 sequences without duplicating their entries") {
            val result = formatYaml(parsed(COLUMN0_YAML))

            listOf("id: foo", "id: 1.0.0", "id: 2.0.0").map { occurrences(result, it) } shouldBe listOf(1, 1, 1)
            result shouldContain "tags:\n  - id: foo"
            result shouldContain "releases:\n  - id: 1.0.0"
            result shouldContain "vulnerabilities: []"
            result shouldContain "# \$schema:"
        }

        test("reformats every entry in place, once and in order") {
            val result = formatYaml(parsed(QUOTED_ENTRIES_YAML))

            listOf("id: CVE-2026-0001", "id: CVE-2026-0002").map { occurrences(result, it) } shouldBe listOf(1, 1)
            result.indexOf("CVE-2026-0001") shouldBeLessThan result.indexOf("CVE-2026-0002")
            occurrences(result, "releases: [1.0.0]") shouldBe 2
            result shouldContain """packages: ["pkg:npm/example-lib@2.3.0"]"""
        }

        test("picks the block style by the value, not by the notation of the source") {
            val result = formatYaml(parsed(STYLED_YAML))

            result shouldContain "analysis: |-\n"
            result shouldContain "comment: >-\n"
        }

        test("drops user comments and writes the canonical schema header") {
            val result = formatYaml(parsed(COMMENTED_YAML))

            result.lines().take(2) shouldBe listOf("# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json", "---")
            result shouldNotContain "reviewed by the security team"
            result shouldNotContain "recheck after upgrade"
        }

        test("renders a flow-style document in the block style") {
            val result = formatYaml(parsed(FLOW_YAML))

            result shouldContain "project:\n  organization: Acme"
            result shouldContain "vulnerabilities:\n\n  - id: CVE-2026-0001"
            result shouldContain "releases: [1.0.0]"
        }

        test("is idempotent for every layout") {
            val layouts = listOf(COLUMN0_YAML, QUOTED_ENTRIES_YAML, STYLED_YAML, COMMENTED_YAML, FLOW_YAML)

            val once = layouts.map { formatYaml(parsed(it)) }
            val twice = once.map { formatYaml(parsed(it)) }

            twice shouldBe once
        }

        test("keeps the deprecated risk acceptable verdict as written") {
            val outcome = formatYamlOutcome(parsed(CANONICAL_LEGACY_VERDICT_YAML))

            outcome shouldBe FormatOutcome.Unchanged
        }

        test("formatYamlOutcome tells canonical content from content it reformats") {
            val canonical = formatYaml(parsed(COLUMN0_YAML))

            val outcomes = listOf(canonical, COLUMN0_YAML).map { formatYamlOutcome(parsed(it)) }

            outcomes shouldBe listOf(FormatOutcome.Unchanged, FormatOutcome.Reformatted(canonical))
        }

        test("the comments-dropped warning names the file and the fields to use instead") {
            val warning = formatCommentsDroppedWarning("web-app.vl.yaml")

            warning shouldBe
                "warning: web-app.vl.yaml: contains YAML comments; they are removed on write\n" +
                "  hint: record notes in schema fields (e.g. comment, analysis)"
        }
    })
