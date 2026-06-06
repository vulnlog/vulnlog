// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

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
// reformatted in place — exactly once, in order — without duplicating any entry.
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
    })
