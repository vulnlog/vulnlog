// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ReportCommandTest :
    FunSpec({

        context("happy path") {

            test("generates an HTML report from a single file") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            ReportCommand().test("${input.absolutePath} -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Report written to:"
                        val html = output.readText()
                        html shouldContain "<!DOCTYPE html>"
                        html shouldContain "CVE-2026-1234"
                    }
                }
            }

            test("merges entries from multiple files of the same project") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogYaml(releaseId = "1.0.0", cveId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogYaml(releaseId = "2.0.0", cveId = "CVE-2026-5678"),
                    ) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ReportCommand().test(
                                    "${f1.absolutePath} ${f2.absolutePath} -o ${output.absolutePath}",
                                )

                            result.statusCode shouldBe 0
                            val html = output.readText()
                            html shouldContain "CVE-2026-1234"
                            html shouldContain "CVE-2026-5678"
                        }
                    }
                }
            }

            test("merges the same CVE from multiple files into a single entry") {
                withTempFile(prefix = "vulnlog-1x", content = vulnlogYaml(releaseId = "1.0.0")) { f1 ->
                    withTempFile(prefix = "vulnlog-2x", content = vulnlogYaml(releaseId = "2.0.0")) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ReportCommand().test(
                                    "${f1.absolutePath} ${f2.absolutePath} -o ${output.absolutePath}",
                                )

                            result.statusCode shouldBe 0
                            val html = output.readText()
                            html shouldContain "CVE-2026-1234"
                            html shouldContain "1.0.0"
                            html shouldContain "2.0.0"
                        }
                    }
                }
            }

            test("reads from stdin when '-' is passed") {
                withTempFile(prefix = "report", suffix = ".html") { output ->
                    withStdin(vulnlogYaml()) {
                        val result = ReportCommand().test("- -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        val html = output.readText()
                        html shouldContain "<!DOCTYPE html>"
                        html shouldContain "CVE-2026-1234"
                    }
                }
            }
        }

        context("input validation") {

            test("fails when no input is provided") {
                val result = ReportCommand().test("")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldBe
                    """
                    Usage: report [<options>] <inputs>...

                    Error: missing argument <inputs>

                    """.trimIndent()
            }

            test("fails when the input file does not exist") {
                val result = ReportCommand().test("/nonexistent/vulnlog.vl.yaml")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldBe
                    """
                    Usage: report [<options>] <inputs>...

                    Error: invalid value for <inputs>: Input path '/nonexistent/vulnlog.vl.yaml' does not exist.

                    """.trimIndent()
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = ReportCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogYaml()) { input ->
                    val result = ReportCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldBe
                        """
                        Usage: report [<options>] <inputs>...

                        Error: invalid value for <inputs>: Input '${input.absolutePath}' is not valid: File name must be [vulnlog|*.vl].[yaml|yml]: ${input.absolutePath}

                        """.trimIndent()
                }
            }

            test("fails when stdin is mixed with file inputs") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withStdin(vulnlogYaml()) {
                        val result = ReportCommand().test("- ${input.absolutePath}")

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "Mixing input files with STDIN is not allowed"
                    }
                }
            }

            test("fails when stdin is given more than once") {
                withStdin(vulnlogYaml()) {
                    val result = ReportCommand().test("- -")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Multiple <stdin> are not supported"
                }
            }
        }

        context("merge validation") {

            test("fails when input files have different project metadata") {
                withTempFile(prefix = "vulnlog-1x", content = vulnlogYaml(projectName = "Project A")) { f1 ->
                    withTempFile(prefix = "vulnlog-2x", content = vulnlogYaml(projectName = "Project B")) { f2 ->
                        val result = ReportCommand().test("${f1.absolutePath} ${f2.absolutePath}")

                        result.statusCode shouldBe ExitCode.VALIDATION_ERROR.ordinal
                        result.stderr shouldContain "same project metadata"
                    }
                }
            }
        }

        context("filter validation") {

            test("fails on an unknown reporter") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = ReportCommand().test("${input.absolutePath} --reporter bogus")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Unsupported reporter: bogus"
                }
            }

            test("fails on an unknown release") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = ReportCommand().test("${input.absolutePath} --release 9.9.9")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.ordinal
                    result.stderr shouldContain "Release not found: 9.9.9"
                    result.stderr shouldContain "Known releases: 1.0.0"
                }
            }

            test("fails on an unknown tag") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = ReportCommand().test("${input.absolutePath} --tag missing-tag")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.ordinal
                    result.stderr shouldContain "Tag not found: missing-tag"
                }
            }
        }

        context("pending fix at deployed release") {

            test("--release including only the deployed release renders an unshipped fix as open") {
                withTempFile(content = vulnlogYamlWithPendingFix()) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            ReportCommand().test(
                                "${input.absolutePath} --release 1.0.0 -o ${output.absolutePath}",
                            )

                        result.statusCode shouldBe 0
                        val html = output.readText()
                        html shouldContain "CVE-2026-9999"
                        html shouldContain "\"state\":\"open\""
                    }
                }
            }

            test("without --release the unshipped fix is rendered as resolved") {
                withTempFile(content = vulnlogYamlWithPendingFix()) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result = ReportCommand().test("${input.absolutePath} -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        val html = output.readText()
                        html shouldContain "CVE-2026-9999"
                        html shouldContain "\"state\":\"resolved\""
                    }
                }
            }
        }
    })
