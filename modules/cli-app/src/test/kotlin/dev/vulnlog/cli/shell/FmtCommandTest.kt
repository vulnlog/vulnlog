// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

// A valid but non-canonical document: fully quoted, block single-element arrays, an inline-long analysis.
private val UGLY_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: "Acme Corp"
      name: "Acme Web App"
      author: "Acme Corp Security Team"

    releases:
      - id: "1.0.0"
        published_at: "2026-01-15"

    vulnerabilities:

      - id: "CVE-2026-1234"
        releases:
          - "1.0.0"
        description: "Remote code execution in example-lib"
        packages:
          - "pkg:npm/example-lib@2.3.0"
        reports:
          - reporter: "trivy"
        analysis: "The vulnerable code path is not reachable in our application because we only use the safe subset."
        verdict: "not affected"
        justification: "vulnerable code not in execute path"
    """.trimIndent() + "\n"

class FmtCommandTest :
    FunSpec({

        context("formatting") {

            test("rewrites a file to the canonical style and preserves the schema header") {
                withTempFile(prefix = "fmt", content = UGLY_YAML) { file ->
                    val result = FmtCommand().test(file.absolutePath)

                    result.statusCode shouldBe 0
                    val content = file.readText()
                    // header preserved
                    content shouldContain "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json"
                    // minimal, type-safe quoting
                    content shouldContain "schemaVersion: \"1\""
                    content shouldContain "organization: Acme Corp"
                    content shouldContain "id: CVE-2026-1234"
                    content shouldContain "verdict: not affected"
                    // single-element scalar list becomes a flow array
                    content shouldContain "releases: [1.0.0]"
                    // colon-bearing purl stays quoted in the flow array
                    content shouldContain """packages: ["pkg:npm/example-lib@2.3.0"]"""
                    // long prose becomes a folded block scalar
                    content shouldContain "analysis: >"
                    // reports (list of mappings) stay block
                    content shouldContain "reports:\n      - reporter: trivy"
                }
            }

            test("is idempotent: formatting an already-formatted file is a no-op") {
                withTempFile(prefix = "fmt", content = UGLY_YAML) { file ->
                    FmtCommand().test(file.absolutePath)
                    val once = file.readText()

                    val result = FmtCommand().test(file.absolutePath)

                    result.statusCode shouldBe 0
                    file.readText() shouldBe once
                    result.stdout shouldContain "Already formatted"
                }
            }

            test("formats multiple files in one invocation") {
                withTempFile(prefix = "a", content = UGLY_YAML) { a ->
                    withTempFile(prefix = "b", content = UGLY_YAML) { b ->
                        val result = FmtCommand().test("${a.absolutePath} ${b.absolutePath}")

                        result.statusCode shouldBe 0
                        a.readText() shouldContain "releases: [1.0.0]"
                        b.readText() shouldContain "releases: [1.0.0]"
                    }
                }
            }
        }

        context("--check") {

            test("exits non-zero and does not modify a file that is not formatted") {
                withTempFile(prefix = "fmt", content = UGLY_YAML) { file ->
                    val result = FmtCommand().test("--check ${file.absolutePath}")

                    result.statusCode shouldBe ExitCode.FORMAT_ERROR.ordinal
                    result.stdout shouldContain "Can be reformatted"
                    // unchanged on disk
                    file.readText() shouldBe UGLY_YAML
                }
            }

            test("exits zero for an already-formatted file") {
                withTempFile(prefix = "fmt", content = UGLY_YAML) { file ->
                    FmtCommand().test(file.absolutePath)

                    val result = FmtCommand().test("--check ${file.absolutePath}")

                    result.statusCode shouldBe 0
                }
            }
        }

        context("stdin/stdout") {

            test("formats STDIN to STDOUT without touching any file") {
                withStdin(UGLY_YAML) {
                    val result = FmtCommand().test("-")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "releases: [1.0.0]"
                    result.stdout shouldContain "# \$schema:"
                }
            }

            test("rejects mixing STDIN with file inputs") {
                withTempFile(prefix = "fmt", content = UGLY_YAML) { file ->
                    val result = FmtCommand().test("- ${file.absolutePath}")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Mixing input files with STDIN is not allowed"
                }
            }

            test("rejects more than one STDIN input") {
                val result = FmtCommand().test("- -")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "Multiple <stdin> are not supported"
            }
        }

        context("invalid input") {

            test("fails to format an unparseable file") {
                withTempFile(prefix = "fmt", content = INVALID_VULNLOG_YAML) { file ->
                    val result = FmtCommand().test(file.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "failed"
                    // file is left untouched
                    file.readText() shouldBe INVALID_VULNLOG_YAML
                }
            }
        }
    })
