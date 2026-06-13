// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

// Uses column-0 block sequences (the `- id:` indicator at the key's indent) under tags and releases.
// This is valid YAML and must be normalised to the indented style without duplicating entries.
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

// Two non-canonical entries (quoted scalars, block single-element arrays) that must each be
// reformatted in place, exactly once and in order, without duplicating any entry.
private val ENTRIES_YAML =
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

// A multi-line analysis and a long single-line comment: styles are a function of the value
// (multi-line -> literal, long single-line -> folded), regardless of the source notation.
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

// User comments anywhere in the file (no header, between sections, inside entries) plus a wrong
// schema pointer: formatting drops the comments and generates the canonical header.
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

// A flow-style vulnerabilities list: valid YAML that the textual splicing approach could not handle.
private val FLOW_YAML =
    """
    schemaVersion: "1"
    project: {organization: Acme, name: App, author: Sec}
    releases: [{id: 1.0.0, published_at: 2026-01-01}]
    vulnerabilities: [{id: CVE-2026-0001, releases: [1.0.0],
      packages: ["pkg:npm/example-lib@2.3.0"], reports: [{reporter: trivy}]}]
    """.trimIndent() + "\n"

class FormatTest :
    FunSpec({
        val mapper = createYamlMapper()

        fun occurrences(
            haystack: String,
            needle: String,
        ) = Regex(Regex.escape(needle)).findAll(haystack).count()

        test("column-0 sequences are normalised without duplicating entries") {
            val result = formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper).content

            // Each entry appears exactly once (the bug duplicated them).
            occurrences(result, "id: foo") shouldBe 1
            occurrences(result, "id: 1.0.0") shouldBe 1
            occurrences(result, "id: 2.0.0") shouldBe 1
            // Normalised to the indented style.
            result shouldContain "tags:\n  - id: foo"
            result shouldContain "releases:\n  - id: 1.0.0"
            // Header preserved.
            result shouldContain "# \$schema:"
        }

        test("formatting is idempotent for column-0 input") {
            val once = formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper)
            val twice = formatYaml(once, mapper)

            twice shouldBe once
        }

        test("formatting is idempotent for already-indented input") {
            val once = formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper)
            // Re-running on indented output is a no-op.
            formatYaml(once, mapper) shouldBe once
        }

        test("reformats every vulnerability entry in place without duplicating it") {
            val result = formatYaml(VulnlogFileRaw(ENTRIES_YAML), mapper).content

            occurrences(result, "id: CVE-2026-0001") shouldBe 1
            occurrences(result, "id: CVE-2026-0002") shouldBe 1
            // single-element scalar lists collapse to flow arrays
            occurrences(result, "releases: [1.0.0]") shouldBe 2
            // colon-bearing purls stay quoted inside the flow array
            result shouldContain """packages: ["pkg:npm/example-lib@2.3.0"]"""
            // header and entry order preserved
            result shouldContain "# \$schema:"
            result.indexOf("CVE-2026-0001") shouldBeLessThan result.indexOf("CVE-2026-0002")
        }

        test("formatting an entry-bearing document is idempotent") {
            val once = formatYaml(VulnlogFileRaw(ENTRIES_YAML), mapper)

            formatYaml(once, mapper) shouldBe once
        }

        test("renders multi-line values as literal and long single-line values as folded blocks") {
            val result = formatYaml(VulnlogFileRaw(STYLED_YAML), mapper).content

            result shouldContain "analysis: |-"
            result shouldContain "comment: >-"
            result shouldNotContain "analysis: >"
        }

        test("formatting a styled document is idempotent") {
            val once = formatYaml(VulnlogFileRaw(STYLED_YAML), mapper)

            formatYaml(once, mapper) shouldBe once
        }

        test("removes user comments and generates the schema header") {
            val result = formatYaml(VulnlogFileRaw(COMMENTED_YAML), mapper).content

            result shouldNotContain "# reviewed by the security team"
            result shouldNotContain "# temporary, recheck after upgrade"
            occurrences(result, "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json") shouldBe 1
            result.lines().first() shouldBe "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json"
            result.lines()[1] shouldBe "---"
        }

        test("renders a flow-style vulnerabilities list in the canonical block style") {
            val result = formatYaml(VulnlogFileRaw(FLOW_YAML), mapper)

            result.content shouldContain "vulnerabilities:\n\n  - id: CVE-2026-0001"
            result.content shouldContain "releases: [1.0.0]"
            formatYaml(result, mapper) shouldBe result
        }

        test("an empty vulnerabilities list renders as an empty flow array") {
            val result = formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper).content

            result shouldContain "vulnerabilities: []"
        }

        test("formatYamlOutcome reports canonical content as unchanged") {
            val canonical = formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper)

            formatYamlOutcome(canonical, mapper) shouldBe FormatOutcome.Unchanged
        }

        test("formatYamlOutcome carries the reformatted content for non-canonical input") {
            val outcome = formatYamlOutcome(VulnlogFileRaw(COLUMN0_YAML), mapper)

            outcome shouldBe FormatOutcome.Reformatted(formatYaml(VulnlogFileRaw(COLUMN0_YAML), mapper))
        }

        test("the comments-dropped warning names the source and the replacement fields") {
            val warning = formatCommentsDroppedWarning("web-app.vl.yaml")

            warning shouldContain "web-app.vl.yaml"
            warning shouldContain "removed on write"
            warning shouldContain "comment"
        }
    })
