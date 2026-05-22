// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class AddVulnerabilitiesCommandTest :
    FunSpec({

        context("STDOUT output") {

            test("prints a list-item YAML entry for a minimal invocation") {
                val result = AddVulnerabilitiesCommand().test("--vuln-id CVE-2026-1234")

                result.statusCode shouldBe 0
                result.stdout shouldContain "  - id: \"CVE-2026-1234\""
                result.stdout shouldContain "releases: []"
                result.stdout shouldContain "packages: []"
                result.stdout shouldNotContain "verdict"
            }

            test("emits multiple releases, packages and tags") {
                val result =
                    AddVulnerabilitiesCommand().test(
                        "--vuln-id CVE-2026-1234 " +
                            "--release 1.0.0 --release 1.1.0 " +
                            "--package pkg:npm/example-lib@2.3.0 " +
                            "--package pkg:npm/other-lib@1.0.0 " +
                            "--tag frontend --tag backend",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "\"1.0.0\""
                result.stdout shouldContain "\"1.1.0\""
                result.stdout shouldContain "pkg:npm/example-lib@2.3.0"
                result.stdout shouldContain "pkg:npm/other-lib@1.0.0"
                result.stdout shouldContain "frontend"
                result.stdout shouldContain "backend"
            }

            test("--reporter adds a reports entry dated today") {
                val result =
                    AddVulnerabilitiesCommand().test(
                        "--vuln-id CVE-2026-1234 --reporter trivy",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "reporter: \"trivy\""
                result.stdout shouldContain "at: \"${LocalDate.now()}\""
            }
        }

        context("argument validation") {

            test("fails when --vuln-id is missing") {
                val result = AddVulnerabilitiesCommand().test("")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--vuln-id"
            }
        }

        context("file write") {

            test("inserts the entry into the destination file and prints a success message") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddVulnerabilitiesCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --release 1.0.0",
                        )

                    result.statusCode shouldBe 0
                    result.stdout shouldBe "Added to ${target.toPath()}: CVE-2026-9999\n"

                    val content = target.readText()
                    content shouldContain "CVE-2026-9999"
                    content shouldContain "CVE-2026-1234"
                }
            }

            test("falls back to the latest published release when --release is omitted") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddVulnerabilitiesCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999",
                        )

                    result.statusCode shouldBe 0
                    val content = target.readText()
                    content shouldContain "CVE-2026-9999"
                    content shouldContain "\"1.0.0\""
                }
            }

            test("writes the entry to multiple destinations") {
                withTempFile(prefix = "target1", content = vulnlogYaml()) { target1 ->
                    withTempFile(prefix = "target2", content = vulnlogYaml()) { target2 ->
                        val result =
                            AddVulnerabilitiesCommand().test(
                                "${target1.absolutePath} ${target2.absolutePath} " +
                                    "--vuln-id CVE-2026-9999",
                            )

                        result.statusCode shouldBe 0
                        target1.readText() shouldContain "CVE-2026-9999"
                        target2.readText() shouldContain "CVE-2026-9999"
                    }
                }
            }

            test("updates an existing entry in place and prints an 'Updated' message") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddVulnerabilitiesCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-1234 " +
                                "--package pkg:npm/example-lib@9.9.9",
                        )

                    result.statusCode shouldBe 0
                    result.stdout shouldBe "Updated in ${target.toPath()}: CVE-2026-1234\n"

                    val content = target.readText()
                    content.split("CVE-2026-1234").size - 1 shouldBe 1
                    content shouldContain "pkg:npm/example-lib@9.9.9"
                    content shouldNotContain "pkg:npm/example-lib@2.3.0"
                    content shouldContain "not affected"
                    content shouldContain "Remote code execution in example-lib"
                }
            }

            test("fails when --release is not defined in the file") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddVulnerabilitiesCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --release 9.9.9",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "not defined"
                }
            }

            test("fails when --tag is not defined in the file") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddVulnerabilitiesCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --tag unknown",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "not defined"
                }
            }
        }
    })
