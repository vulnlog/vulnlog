// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldNotStartWith

class SuppressCommandTest :
    FunSpec({

        context("encoding validation") {

            test("invalid XML notes fail before any output is written and generic remains available") {
                val yaml =
                    vulnlogDocument(reporter = "dependency-check")
                        .replace("    verdict:", "    analysis: \"before\\0\\uFFFFafter\"\n    verdict:")
                        .replace(
                            "      - reporter: dependency-check",
                            "      - reporter: dependency-check\n      - reporter: trivy",
                        )
                val second = yaml.substringAfter("vulnerabilities:\n").replace("CVE-2026-1234", "CVE-2026-5678")
                withTempFile(content = yaml + second) { input ->
                    withTempDir(prefix = "invalid-xml-out") { outputDir ->
                        val target = outputDir.resolve("dependency-check-suppressions.xml").toFile()
                        target.writeText("previous content")
                        val command = "${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}"

                        val native = SuppressCommand().test(command)

                        native.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                        native.stderr shouldContain "character U+0000 is not allowed in XML 1.0"
                        native.stderr shouldContain "character U+FFFF is not allowed in XML 1.0"
                        native.stderr shouldContain "cannot encode analysis for CVE-2026-1234"
                        native.stderr shouldContain "cannot encode analysis for CVE-2026-5678"
                        target.readText() shouldBe "previous content"
                        outputDir.resolve(".trivyignore.yaml").toFile().exists() shouldBe false

                        val generic = SuppressCommand().test("$command --format generic")

                        generic.statusCode shouldBe 0
                        outputDir.resolve("dependency-check.generic.json").toFile().readText() shouldContain
                            "before\\u0000"
                    }
                }
            }
        }

        context("happy path") {

            test("dependency-check auto writes a GHSA-only finding as XML") {
                val yaml = vulnlogDocument(reporter = "dependency-check", vulnId = "GHSA-aaaa-bbbb-cccc")
                withTempFile(content = yaml) { input ->
                    withTempDir(prefix = "dependency-check-ghsa-out") { outputDir ->
                        val result =
                            SuppressCommand().test(
                                "${input.absolutePath} --format auto --output-dir ${outputDir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        val xml = outputDir.resolve("dependency-check-suppressions.xml").toFile().readText()
                        xml shouldContain "<vulnerabilityName>GHSA-aaaa-bbbb-cccc</vulnerabilityName>"
                        xml shouldNotContain "<cve>"
                        result.stderr shouldNotContain "unsupported"
                    }
                }
            }

            test("dependency-check auto writes XML and generic keeps JSON") {
                withTempFile(content = vulnlogDocument(reporter = "dependency-check")) { input ->
                    withTempDir(prefix = "dependency-check-out") { outputDir ->
                        val command = "${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}"
                        val native = SuppressCommand().test(command)

                        native.statusCode shouldBe 0
                        outputDir.resolve("dependency-check-suppressions.xml").toFile().readText() shouldContain
                            "<cve>CVE-2026-1234</cve>"
                        outputDir.resolve("dependency-check.generic.json").toFile().exists() shouldBe false

                        val generic = SuppressCommand().test("$command --format generic")

                        generic.statusCode shouldBe 0
                        outputDir.resolve("dependency-check.generic.json").toFile().readText() shouldContain
                            "\"id\" : \"CVE-2026-1234\""
                    }
                }
            }

            test("-o writes the single applicable suppression file to the user-specified path") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val target = outputDir.resolve("my-ignore.yaml")
                        val result =
                            SuppressCommand().test("${input.absolutePath} -o ${target.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Wrote: ${target.toAbsolutePath()}"
                        target.toFile().exists() shouldBe true
                    }
                }
            }

            test("writes to stdout when -o is '-'") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1234"
                }
            }

            test("reads from stdin and writes to stdout") {
                withStdin(vulnlogDocument()) {
                    val result = SuppressCommand().test("- -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1234"
                }
            }

            test("falls back to the current working directory when no output flag is given") {
                withTempFile(content = vulnlogDocument()) { input ->
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
                        result.stderr shouldContain "Wrote: "
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

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
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
                withTempFile(content = vulnlogDocument()) { input ->
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

            test("exits success with an informational message when nothing is suppressible") {
                withTempFile(content = vulnlogYamlOtherReporterOnly()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test("${input.absolutePath} --output-dir ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Unchanged: no suppression entries applicable"
                        val written = outputDir.toFile().listFiles().orEmpty()
                        written.toList() shouldBe emptyList()
                    }
                }
            }
        }

        context("output path validation") {

            test("--output-dir fails when the path is not a directory") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result =
                        SuppressCommand().test("${input.absolutePath} --output-dir /nonexistent/suppress-out")

                    result.statusCode shouldNotBe 0
                    result.stderr shouldContain "is not a directory"
                }
            }

            test("-o fails when the path is a directory") {
                withTempFile(content = vulnlogDocument()) { input ->
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

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                result.stderr shouldContain "does not exist"
            }

            test("fails when the input path is a directory") {
                withTempDir { dir ->
                    val result = SuppressCommand().test(dir.toAbsolutePath().toString())

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "is a directory"
                }
            }

            test("fails when the input file name does not match the expected pattern") {
                withTempFile(prefix = "invalid-name", suffix = ".txt", content = vulnlogDocument()) { input ->
                    val result = SuppressCommand().test(input.absolutePath)

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.code
                    result.stderr shouldContain "File name must be"
                }
            }

            test("fails on invalid Vulnlog YAML from stdin") {
                withStdin(INVALID_VULNLOG_YAML) {
                    val result = SuppressCommand().test("-")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: <stdin>: "
                }
            }
        }

        context("filter validation") {

            test("names every bad filter value at once") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result =
                        SuppressCommand().test(
                            "${input.absolutePath} --reporter dependency_check --as-of 9.9.9 --tag missing-tag",
                        )

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "error: Invalid reporter: dependency_check"
                    result.stderr shouldContain "error: Release not found: 9.9.9"
                    result.stderr shouldContain "hint: Known releases: 1.0.0"
                    result.stderr shouldContain "error: Tag not found: missing-tag"
                }
            }

            test("points at --as-of when the renamed --release is used") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --release 1.0.0")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Option --release was renamed to --as-of."
                }
            }

            test("--reporter help text lists the canonical hyphenated names") {
                val result = SuppressCommand().test("--help")

                result.stdout shouldContain "dependency-check"
                result.stdout shouldContain "github-dependabot"
                result.stdout shouldContain "npm-audit"
                result.stdout shouldContain "cargo-audit"
            }
        }

        context("uniform output formatting") {

            test("does not print a leading blank line on stderr") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stderr shouldNotStartWith "\n"
                }
            }

            test("does not print INFO-level validation findings") {
                withTempFile(content = ValidationDocuments.UNREFERENCED_RELEASE) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stderr shouldNotContain "info: ${input.name}: "
                }
            }
        }

        context("format selection") {

            test("--format generic writes the generic JSON file for a native reporter") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test(
                                "${input.absolutePath} --format generic --output-dir ${outputDir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        outputDir.resolve("trivy.generic.json").toFile().exists() shouldBe true
                        outputDir.resolve(".trivyignore.yaml").toFile().exists() shouldBe false
                    }
                }
            }

            test("--format is case insensitive") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test(
                                "${input.absolutePath} --format GENERIC --output-dir ${outputDir.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        outputDir.resolve("trivy.generic.json").toFile().exists() shouldBe true
                    }
                }
            }

            test("rejects an unknown format value") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = SuppressCommand().test("${input.absolutePath} --format bogus")

                    result.statusCode shouldNotBe 0
                    result.stderr shouldContain "--format"
                    result.stderr shouldContain "auto"
                    result.stderr shouldContain "generic"
                }
            }
        }
    })
