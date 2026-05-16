// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SuppressCommandTest :
    FunSpec({

        context("happy path") {

            test("writes a suppression file to the configured directory") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test("${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Suppression file created at:"
                    }
                }
            }

            test("-o writes the single applicable suppression file to the user-specified path") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val target = outputDir.resolve("my-ignore.yaml")
                        val result =
                            SuppressCommand().test("${input.absolutePath} -o ${target.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Suppression file created at: ${target.toAbsolutePath()}"
                        target.toFile().exists() shouldBe true
                    }
                }
            }

            test("writes to stdout when -o is '-'") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1234"
                }
            }

            test("reads from stdin and writes to stdout") {
                withStdin(vulnlogYaml()) {
                    val result = SuppressCommand().test("- -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1234"
                }
            }

            test("reads from stdin and writes to a directory") {
                withTempDir(prefix = "suppress-out") { outputDir ->
                    withStdin(vulnlogYaml()) {
                        val result = SuppressCommand().test("- --output-dir ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Suppression file created at:"
                    }
                }
            }

            test("falls back to the current working directory when no output flag is given") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-cwd") { cwd ->
                        val result = withCwd(cwd) { SuppressCommand().test(input.absolutePath) }

                        result.statusCode shouldBe 0
                        cwd.resolve(".trivyignore.yaml").toFile().exists() shouldBe true
                    }
                }
            }

            test("--output-dir writes one file per applicable reporter") {
                withTempFile(content = vulnlogYamlMultiReporter()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test("${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        outputDir.resolve(".trivyignore.yaml").toFile().exists() shouldBe true
                        outputDir.resolve(".snyk").toFile().exists() shouldBe true
                    }
                }
            }
        }

        context("single-file mode requires a single reporter") {

            test("-o fails when multiple reporters apply and --reporter is not given") {
                withTempFile(content = vulnlogYamlMultiReporter()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "-o requires a single reporter"
                    result.stderr shouldContain "snyk"
                    result.stderr shouldContain "trivy"
                }
            }

            test("-o with --reporter succeeds when multiple reporters apply") {
                withTempFile(content = vulnlogYamlMultiReporter()) { input ->
                    val result =
                        SuppressCommand().test("${input.absolutePath} --reporter trivy -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1234"
                }
            }

            test("-o and --output-dir are mutually exclusive") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test(
                                "${input.absolutePath} -o - --output-dir ${outputDir.toAbsolutePath()}",
                            )

                        result.statusCode shouldNotBe 0
                        result.stderr shouldContain "cannot be used with"
                    }
                }
            }

            test("-o exits success with an informational message when nothing is suppressible") {
                withTempFile(content = vulnlogYamlOtherReporterOnly()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "No suppression entries applicable"
                    result.stdout shouldNotContain "CVE-2026-1234"
                }
            }

            test("--output-dir exits success with an informational message when nothing is suppressible") {
                withTempFile(content = vulnlogYamlOtherReporterOnly()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test("${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "No suppression entries applicable"
                        val written = outputDir.toFile().listFiles().orEmpty()
                        written.toList() shouldBe emptyList()
                    }
                }
            }
        }

        context("output path validation") {

            test("--output-dir fails when the path is not a directory") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result =
                        SuppressCommand().test("${input.absolutePath} --output-dir /nonexistent/suppress-out")

                    result.statusCode shouldNotBe 0
                    result.stderr shouldContain "is not a directory"
                }
            }

            test("-o fails when the path is a directory") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result = SuppressCommand().test("${input.absolutePath} -o ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldNotBe 0
                        result.stderr shouldContain "is a directory, expected a file"
                    }
                }
            }
        }

        context("input validation") {

            test("fails when the input file does not exist") {
                val result = SuppressCommand().test("/nonexistent/vulnlog.vl.yaml")

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "does not exist"
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = SuppressCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "File name must be"
                }
            }

            test("fails on invalid Vulnlog YAML from stdin") {
                withStdin(INVALID_VULNLOG_YAML) {
                    val result = SuppressCommand().test("-")

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "Parsing of <stdin> failed"
                }
            }
        }

        context("filter validation") {

            context("--reporter accepts the canonical hyphenated names") {
                listOf(
                    "trivy",
                    "snyk",
                    "dependency-check",
                    "github-advisory",
                    "grype",
                    "npm-audit",
                    "cargo-audit",
                    "semgrep",
                    "other",
                ).forEach { reporter ->
                    test("--reporter $reporter is accepted") {
                        withTempFile(content = vulnlogYaml()) { input ->
                            withTempDir(prefix = "suppress-out") { outputDir ->
                                val result =
                                    SuppressCommand().test(
                                        "${input.absolutePath} --reporter $reporter " +
                                            "--output-dir ${outputDir.toAbsolutePath()}",
                                    )

                                result.statusCode shouldBe 0
                            }
                        }
                    }
                }
            }

            test("--reporter rejects underscored names without leaking internals") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --reporter dependency_check")

                    result.statusCode shouldBe 1
                    result.stderr shouldContain "Unsupported reporter: dependency_check"
                    result.stderr shouldNotContain "dev.vulnlog"
                    result.stderr shouldNotContain "No enum constant"
                }
            }

            test("--reporter rejects unknown values without leaking internals") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --reporter bogus")

                    result.statusCode shouldBe 1
                    result.stderr shouldContain "Unsupported reporter: bogus"
                    result.stderr shouldNotContain "dev.vulnlog"
                }
            }

            test("fails on an unknown release") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --release 9.9.9")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.ordinal
                    result.stderr shouldContain "Release not found: 9.9.9"
                    result.stderr shouldContain "Known releases: 1.0.0"
                }
            }

            test("fails on an unknown tag") {
                withTempFile(content = vulnlogYaml()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --tag missing-tag")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.ordinal
                    result.stderr shouldContain "Tag not found: missing-tag"
                }
            }

            test("--reporter help text lists the canonical hyphenated names") {
                val result = SuppressCommand().test("--help")

                result.stdout shouldContain "dependency-check"
                result.stdout shouldContain "github-advisory"
                result.stdout shouldContain "npm-audit"
                result.stdout shouldContain "cargo-audit"
            }
        }

        context("pending fix at deployed release") {

            test("--release including only the deployed release suppresses an affected CVE whose fix is unshipped") {
                withTempFile(content = vulnlogYamlWithPendingFix()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --release 1.0.0 -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-9999"
                }
            }

            test("without --release the affected CVE with a resolution is dropped") {
                withTempFile(content = vulnlogYamlWithPendingFix()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldNotContain "CVE-2026-9999"
                }
            }
        }
    })
