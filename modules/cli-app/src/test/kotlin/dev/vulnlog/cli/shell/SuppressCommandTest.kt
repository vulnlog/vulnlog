// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SuppressCommandTest :
    FunSpec({

        context("happy path") {

            test("writes a suppression file to the configured directory") {
                withTempFile(content = vulnlogYaml()) { input ->
                    withTempDir(prefix = "suppress-out") { outputDir ->
                        val result =
                            SuppressCommand().test("${input.absolutePath} -o ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Suppression file created at:"
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
                        val result = SuppressCommand().test("- -o ${outputDir.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Suppression file created at:"
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
                                        "${input.absolutePath} --reporter $reporter -o ${outputDir.toAbsolutePath()}",
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
