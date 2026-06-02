// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
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

class FormatTest :
    FunSpec({
        val mapper = createYamlMapper()

        fun occurrences(
            haystack: String,
            needle: String,
        ) = Regex(Regex.escape(needle)).findAll(haystack).count()

        test("column-0 sequences are normalised without duplicating entries") {
            val result = formatYaml(COLUMN0_YAML, mapper)

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
            val once = formatYaml(COLUMN0_YAML, mapper)
            val twice = formatYaml(once, mapper)

            twice shouldBe once
        }

        test("formatting is idempotent for already-indented input") {
            val once = formatYaml(COLUMN0_YAML, mapper)
            // Re-running on indented output is a no-op.
            formatYaml(once, mapper) shouldBe once
        }
    })
