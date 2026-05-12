// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ValidateCommandTest :
    FunSpec({

        context("happy path") {

            test("succeeds on a valid Vulnlog file") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "Validation OK"
                }
            }

            test("succeeds on multiple valid Vulnlog files") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogYaml(releaseId = "1.0.0", cveId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogYaml(releaseId = "2.0.0", cveId = "CVE-2026-5678"),
                    ) { f2 ->
                        val result = ValidateCommand().test("${f1.absolutePath} ${f2.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Validation OK"
                    }
                }
            }

            test("reads from stdin when '-' is passed") {
                withStdin(vulnlogYaml()) {
                    val result = ValidateCommand().test("-")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "Validation OK"
                }
            }
        }

        context("input validation") {

            test("fails when no input is provided") {
                val result = ValidateCommand().test("")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldBe
                    """
                    Usage: validate [<options>] <inputs>...

                    Error: missing argument <inputs>

                    """.trimIndent()
            }

            test("fails when the input file does not exist") {
                val result = ValidateCommand().test("/nonexistent/vulnlog.vl.yaml")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "does not exist"
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = ValidateCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogYaml()) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "File name must be"
                }
            }

            test("fails when stdin is mixed with file inputs") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withStdin(vulnlogYaml()) {
                        val result = ValidateCommand().test("- ${input.absolutePath}")

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "Mixing input files with STDIN is not allowed"
                    }
                }
            }

            test("fails when stdin is given more than once") {
                withStdin(vulnlogYaml()) {
                    val result = ValidateCommand().test("- -")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Multiple <stdin> are not supported"
                }
            }
        }

        context("parse failures") {

            test("reports parse failure for an invalid Vulnlog file") {
                withTempFile(content = INVALID_VULNLOG_YAML) { input ->
                    val result = ValidateCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Parsing of ${input.name} failed"
                }
            }

            test("reports parse failure for invalid Vulnlog YAML on stdin") {
                withStdin(INVALID_VULNLOG_YAML) {
                    val result = ValidateCommand().test("-")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Parsing of <stdin> failed"
                }
            }
        }
    })
