// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlin.io.path.readText

private val AFFECTED_VERDICT =
    """
    |    verdict: affected
    |    severity: high
    """.trimMargin()

private val WONT_FIX_VERDICT =
    """
    |    verdict: affected
    |    severity: low
    |    disposition: wont fix
    """.trimMargin()

private val WILL_FIX_VERDICT =
    """
    |    verdict: affected
    |    severity: high
    |    disposition: will fix
    """.trimMargin()

class ImpactReportCommandTest :
    FunSpec({

        context("happy path") {

            test("generates an HTML report from a single file") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            ImpactReportCommand().test("${input.absolutePath} -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Wrote: "
                        val html = output.readText()
                        html shouldContain "<!DOCTYPE html>"
                        html shouldContain "CVE-2026-1234"
                    }
                }
            }

            test("merges entries from multiple files of the same project") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogDocument(releaseId = "1.0.0", vulnId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogDocument(releaseId = "2.0.0", vulnId = "CVE-2026-5678"),
                    ) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ImpactReportCommand().test(
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

            test("reads from stdin when '-' is passed") {
                withTempFile(prefix = "report", suffix = ".html") { output ->
                    withStdin(vulnlogDocument()) {
                        val result = ImpactReportCommand().test("- -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        val html = output.readText()
                        html shouldContain "<!DOCTYPE html>"
                        html shouldContain "CVE-2026-1234"
                    }
                }
            }
        }

        context("output") {

            test("writes HTML to vulnlog-impact-report.html in the current directory when no output is given") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempDir(prefix = "impact-cwd") { cwd ->
                        val result = withCwd(cwd) { ImpactReportCommand().test(input.absolutePath) }

                        result.statusCode shouldBe 0
                        result.stdout shouldBe ""
                        cwd.resolve("vulnlog-impact-report.html").readText() shouldContain "<!DOCTYPE html>"
                    }
                }
            }

            test("writes JSON to stdout when no output is given") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --format json")

                    result.statusCode shouldBe 0
                    result.stdout shouldStartWith "{"
                    result.stdout shouldContain "CVE-2026-1234"
                    result.stderr shouldNotContain "Wrote: "
                }
            }

            test("writes the plain table to stdout when no output is given and stdout is no terminal") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --format table")

                    result.statusCode shouldBe 0
                    result.stdout shouldStartWith "Vulnerability Impact Report: "
                    result.stdout shouldContain "│ CVE-2026-1234"
                    result.stdout
                        .lines()
                        .first { it.startsWith("┌") }
                        .length shouldBe 120
                    result.stdout shouldNotContain "\u001B["
                    result.stdout shouldEndWith "┘\n"
                    result.stderr shouldNotContain "Wrote: "
                }
            }

            test("fits the table to the terminal width and colors it when stdout is a terminal") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result =
                        ImpactReportCommand().test(
                            "${input.absolutePath} --format table",
                            ansiLevel = AnsiLevel.ANSI16,
                            width = 90,
                            outputInteractive = true,
                        )

                    val plain = result.stdout.replace(Regex("\u001B\\[[0-9;]*m"), "")

                    result.statusCode shouldBe 0
                    plain.lines().first { it.startsWith("┌") }.length shouldBe 90
                    result.stdout shouldContain "\u001B["
                }
            }

            test("writes the plain table to the output file, also when stdout is a terminal") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempFile(prefix = "report", suffix = ".txt") { output ->
                        val result =
                            ImpactReportCommand().test(
                                "${input.absolutePath} --format table -o ${output.absolutePath}",
                                ansiLevel = AnsiLevel.ANSI16,
                                width = 90,
                                outputInteractive = true,
                            )

                        val table = output.readText()

                        result.statusCode shouldBe 0
                        table shouldNotContain "\u001B["
                        table.lines().first { it.startsWith("┌") }.length shouldBe 120
                        table shouldEndWith "┘\n"
                    }
                }
            }

            test("writes JSON to the output file when one is given") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempFile(prefix = "report", suffix = ".json") { output ->
                        val result =
                            ImpactReportCommand().test("${input.absolutePath} --format json -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stdout shouldBe ""
                        result.stderr shouldContain "Wrote: "
                        output.readText() shouldStartWith "{"
                    }
                }
            }

            test("writes HTML to stdout when the output is '-'") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "<!DOCTYPE html>"
                    result.stderr shouldNotContain "Wrote: "
                }
            }

            test("names the default of each format in the help text") {
                val result = ImpactReportCommand().test("--help")

                unwrapped(result.stdout) shouldContain
                    "Defaults to vulnlog-impact-report.html in the current directory for html, " +
                    "and to stdout for json and table."
            }
        }

        context("diagnostics") {

            test("-v shows the parsed inputs and the written output") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            vulnlogCommand().test(
                                "-v report impact ${input.absolutePath} -o ${output.absolutePath}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain
                            "verbose: parsed ${input.name}: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                        result.stderr shouldContain "verbose: wrote ${output.absolutePath}"
                    }
                }
            }

            test("does not print INFO-level validation findings") {
                withTempFile(content = ValidationDocuments.UNREFERENCED_RELEASE) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            ImpactReportCommand().test(
                                "${input.absolutePath} -o ${output.absolutePath}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldNotContain "info: ${input.name}: "
                    }
                }
            }
        }

        context("input validation") {

            test("fails when no input is provided") {
                val result = ImpactReportCommand().test("")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                result.stderr shouldBe
                    """
                    Usage: impact [<options>] <inputs>...

                    Error: missing argument <inputs>

                    """.trimIndent()
            }

            test("fails when the input file does not exist") {
                val result = ImpactReportCommand().test("/nonexistent/vulnlog.vl.yaml")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                result.stderr shouldBe
                    """
                    Usage: impact [<options>] <inputs>...

                    Error: invalid value for <inputs>: Input path '/nonexistent/vulnlog.vl.yaml' does not exist.

                    """.trimIndent()
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = ImpactReportCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails with a named error and a hint when the output directory does not exist") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempDir { dir ->
                        val output = dir.resolve("missing").resolve("report.html")

                        val result = ImpactReportCommand().test("${input.absolutePath} -o $output")

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                        result.stderr shouldBe
                            "error: cannot write $output: its directory does not exist\n" +
                            "  hint: create the directory first\n"
                    }
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldBe
                        """
                        Usage: impact [<options>] <inputs>...

                        Error: invalid value for <inputs>: Input '${input.absolutePath}' is not valid: File name must be [vulnlog|*.vl].[yaml|yml]: ${input.absolutePath}

                        """.trimIndent()
                }
            }

            test("fails when stdin is mixed with file inputs") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withStdin(vulnlogDocument()) {
                        val result = ImpactReportCommand().test("- ${input.absolutePath}")

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                        result.stderr shouldContain "Mixing input files with STDIN is not allowed"
                    }
                }
            }

            test("fails when stdin is given more than once") {
                withStdin(vulnlogDocument()) {
                    val result = ImpactReportCommand().test("- -")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "Multiple <stdin> are not supported"
                }
            }
        }

        context("merge validation") {

            test("fails when input files have different project metadata") {
                withTempFile(prefix = "vulnlog-1x", content = vulnlogDocument(projectName = "Project A")) { f1 ->
                    withTempFile(prefix = "vulnlog-2x", content = vulnlogDocument(projectName = "Project B")) { f2 ->
                        val result = ImpactReportCommand().test("${f1.absolutePath} ${f2.absolutePath}")

                        result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                        result.stderr shouldContain "same project metadata"
                    }
                }
            }
        }

        context("state filter") {

            test("fails on a state Vulnlog does not define") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --state bogus")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Invalid state: bogus"
                    result.stderr shouldContain "Supported states:"
                    result.stderr shouldNotContain "dev.vulnlog"
                }
            }

            test("repeating the flag selects the union of both states") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogDocument(vulnId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogDocument(vulnId = "CVE-2026-5678", verdictBlock = AFFECTED_VERDICT),
                    ) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ImpactReportCommand().test(
                                    "${f1.absolutePath} ${f2.absolutePath} -o ${output.absolutePath} " +
                                        "--state open --state 'not applicable'",
                                )

                            result.statusCode shouldBe 0
                            val html = output.readText()
                            html shouldContain "CVE-2026-1234"
                            html shouldContain "CVE-2026-5678"
                        }
                    }
                }
            }

            test("lists the supported states in the help text") {
                val result = ImpactReportCommand().test("--help")

                unwrapped(result.stdout) shouldContain
                    "Supported states: under investigation, open, accepted, resolved, not applicable"
            }
        }

        context("verdict filter") {

            test("fails on a verdict Vulnlog does not define") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --verdict bogus")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Invalid verdict: bogus"
                    result.stderr shouldContain "Supported verdicts:"
                    result.stderr shouldNotContain "dev.vulnlog"
                }
            }

            test("repeating the flag selects the union of both verdicts") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogDocument(vulnId = "CVE-2026-1234"),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogDocument(vulnId = "CVE-2026-5678", verdictBlock = AFFECTED_VERDICT),
                    ) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ImpactReportCommand().test(
                                    "${f1.absolutePath} ${f2.absolutePath} -o ${output.absolutePath} " +
                                        "--verdict affected --verdict 'not affected'",
                                )

                            result.statusCode shouldBe 0
                            val html = output.readText()
                            html shouldContain "CVE-2026-1234"
                            html shouldContain "CVE-2026-5678"
                        }
                    }
                }
            }

            test("lists the supported verdicts in the help text") {
                val result = ImpactReportCommand().test("--help")

                unwrapped(result.stdout) shouldContain
                    "Supported verdicts: under investigation, affected, not affected"
            }
        }

        context("disposition filter") {

            test("fails on a disposition Vulnlog does not define") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --disposition bogus")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Invalid disposition: bogus"
                    result.stderr shouldContain "Supported dispositions:"
                    result.stderr shouldNotContain "dev.vulnlog"
                }
            }

            test("repeating the flag selects the union of both intents") {
                withTempFile(
                    prefix = "vulnlog-1x",
                    content = vulnlogDocument(vulnId = "CVE-2026-1234", verdictBlock = WONT_FIX_VERDICT),
                ) { f1 ->
                    withTempFile(
                        prefix = "vulnlog-2x",
                        content = vulnlogDocument(vulnId = "CVE-2026-5678", verdictBlock = WILL_FIX_VERDICT),
                    ) { f2 ->
                        withTempFile(prefix = "report", suffix = ".html") { output ->
                            val result =
                                ImpactReportCommand().test(
                                    "${f1.absolutePath} ${f2.absolutePath} -o ${output.absolutePath} " +
                                        "--disposition 'will fix' --disposition 'wont fix'",
                                )

                            result.statusCode shouldBe 0
                            val html = output.readText()
                            html shouldContain "CVE-2026-1234"
                            html shouldContain "CVE-2026-5678"
                        }
                    }
                }
            }

            test("lists the supported dispositions in the help text") {
                val result = ImpactReportCommand().test("--help")

                unwrapped(result.stdout) shouldContain "Supported dispositions: will fix, wont fix"
            }
        }

        context("filter validation") {

            test("fails on an unknown reporter") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --reporter bogus")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Invalid reporter: bogus"
                    result.stderr shouldContain "Supported reporters:"
                }
            }

            test("fails on an unknown release") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --as-of 9.9.9")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Release not found: 9.9.9"
                    result.stderr shouldContain "Known releases: 1.0.0"
                }
            }

            test("a known reporter selects the entries it reported") {
                withTempFile(content = vulnlogDocument(reporter = "trivy")) { input ->
                    withTempFile(prefix = "report", suffix = ".html") { output ->
                        val result =
                            ImpactReportCommand().test(
                                "${input.absolutePath} --reporter trivy -o ${output.absolutePath}",
                            )

                        result.statusCode shouldBe 0
                        output.readText() shouldContain "CVE-2026-1234"
                    }
                }
            }

            test("points at --as-of when the renamed --release is used") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --release 1.0.0")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Option --release was renamed to --as-of."
                    result.stderr shouldContain "--as-of 1.0.0"
                }
            }

            test("fails on an unknown tag") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ImpactReportCommand().test("${input.absolutePath} --tag missing-tag")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Tag not found: missing-tag"
                }
            }
        }
    })
