// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotStartWith

class ValidateCommandTest :
    FunSpec({

        context("happy path") {

            test("succeeds on a valid Vulnlog file") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "Validated: ${input.name}"
                }
            }

            test("succeeds on multiple valid Vulnlog files") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogDocument(releaseId = "1.0.0", vulnId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogDocument(releaseId = "2.0.0", vulnId = "CVE-2026-5678"),
                    ) { f2 ->
                        val result = ValidateCommand().test("${f1.absolutePath} ${f2.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Validated: ${f1.name}"
                        result.stderr shouldContain "Validated: ${f2.name}"
                    }
                }
            }

            test("reads from stdin when '-' is passed") {
                withStdin(vulnlogDocument()) {
                    val result = ValidateCommand().test("-")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "Validated: <stdin>"
                }
            }

            test("does not print a leading blank line on stderr") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stderr shouldNotStartWith "\n"
                }
            }

            test("prints INFO-level findings for files with informational observations") {
                withTempFile(content = ValidationDocuments.UNREFERENCED_RELEASE) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "info: ${input.name}: "
                    result.stderr shouldContain "Unreferenced release ID"
                    result.stderr shouldContain "Validated: ${input.name}"
                }
            }
        }

        context("warnings") {

            test("succeeds with warnings when --strict is not given") {
                withTempFile(content = ValidationDocuments.ANALYZED_BEFORE_REPORTED) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "warning: ${input.name}: "
                    result.stderr shouldContain "Validated: ${input.name}"
                }
            }

            test("fails with warnings when --strict is given") {
                withTempFile(content = ValidationDocuments.ANALYZED_BEFORE_REPORTED) { input ->
                    val result = ValidateCommand().test("--strict ${input.absolutePath}")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "warning: ${input.name}: "
                }
            }
        }

        context("diagnostics") {

            test("-v shows the parsed input and the per-file validation summary") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = vulnlogCommand().test("-v validate ${input.absolutePath}")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain
                        "verbose: parsed ${input.name}: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                    result.stderr shouldContain "verbose: validated ${input.name}: no findings"
                }
            }
        }

        context("input validation") {

            test("fails when no input is provided") {
                val result = ValidateCommand().test("")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                result.stderr shouldBe
                    """
                    Usage: validate [<options>] <inputs>...

                    Error: missing argument <inputs>

                    """.trimIndent()
            }

            test("fails when the input file does not exist") {
                val result = ValidateCommand().test("/nonexistent/vulnlog.vl.yaml")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                result.stderr shouldContain "does not exist"
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = ValidateCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogDocument()) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "File name must be"
                }
            }

            test("fails when stdin is mixed with file inputs") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withStdin(vulnlogDocument()) {
                        val result = ValidateCommand().test("- ${input.absolutePath}")

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                        result.stderr shouldContain "Mixing input files with STDIN is not allowed"
                    }
                }
            }

            test("fails when stdin is given more than once") {
                withStdin(vulnlogDocument()) {
                    val result = ValidateCommand().test("- -")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "Multiple <stdin> are not supported"
                }
            }
        }

        context("parse failures") {

            test("reports parse failure for an invalid Vulnlog file") {
                withTempFile(content = INVALID_VULNLOG_YAML) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: ${input.name}: "
                }
            }

            test("reports parse failure for invalid Vulnlog YAML on stdin") {
                withStdin(INVALID_VULNLOG_YAML) {
                    val result = ValidateCommand().test("-")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: <stdin>: "
                }
            }

            test("reports the failure location for a YAML syntax error") {
                withTempFile(content = "schemaVersion: [unclosed") { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain Regex("error: ${Regex.escape(input.name)}: \\d+:\\d+: ")
                }
            }

            test("reports domain mapping failures with their path and location") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: UNKNOWN-2021-0001
                        releases: []
                        packages: []
                        reports: []
                    """.trimIndent()
                withTempFile(content = yaml) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "vulnerabilities[UNKNOWN-2021-0001].id"
                    result.stderr shouldContain Regex("error: ${Regex.escape(input.name)}: \\d+:\\d+: ")
                }
            }
        }
    })
