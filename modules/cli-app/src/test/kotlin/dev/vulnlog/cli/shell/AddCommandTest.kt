// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class AddCommandTest :
    FunSpec({

        context("STDOUT output") {

            test("prints a list-item YAML entry for a minimal invocation") {
                val result = AddCommand().test("--vuln-id CVE-2026-1234")

                result.statusCode shouldBe 0
                result.stdout shouldContain "  - id: CVE-2026-1234"
                result.stdout shouldContain "releases: []"
                result.stdout shouldContain "packages: []"
                result.stdout shouldNotContain "verdict"
            }

            test("emits multiple releases, packages and tags") {
                val result =
                    AddCommand().test(
                        "--vuln-id CVE-2026-1234 " +
                            "--release 1.0.0 --release 1.1.0 " +
                            "--package pkg:npm/example-lib@2.3.0 " +
                            "--package pkg:npm/other-lib@1.0.0 " +
                            "--tag frontend --tag backend",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "- 1.0.0"
                result.stdout shouldContain "- 1.1.0"
                result.stdout shouldContain "pkg:npm/example-lib@2.3.0"
                result.stdout shouldContain "pkg:npm/other-lib@1.0.0"
                result.stdout shouldContain "frontend"
                result.stdout shouldContain "backend"
            }

            test("--reporter adds a reports entry dated today") {
                val result =
                    AddCommand().test(
                        "--vuln-id CVE-2026-1234 --reporter trivy",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "reporter: trivy"
                result.stdout shouldContain "at: ${LocalDate.now()}"
            }

            test("adds a report for every --reporter") {
                val result =
                    AddCommand().test(
                        "--vuln-id CVE-2026-1234 --reporter trivy --reporter snyk",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "reporter: trivy"
                result.stdout shouldContain "reporter: snyk"
            }

            test("emits the scalar and metadata fields") {
                val result =
                    AddCommand().test(
                        "--vuln-id CVE-2026-1234 --name Log4Shell " +
                            "--description \"Remote code execution.\" " +
                            "--verdict \"not affected\" " +
                            "--justification \"vulnerable code not in execute path\" " +
                            "--comment \"Revisit later.\"",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "name: Log4Shell"
                result.stdout shouldContain "description: Remote code execution."
                result.stdout shouldContain "verdict: not affected"
                result.stdout shouldContain "justification: vulnerable code not in execute path"
                result.stdout shouldContain "comment: Revisit later."
            }
        }

        context("argument validation") {

            test("fails when --vuln-id is missing") {
                val result = AddCommand().test("")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--vuln-id"
            }

            test("fails on an unknown --verdict value") {
                val result = AddCommand().test("--vuln-id CVE-2026-1234 --verdict bogus")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--verdict"
            }
        }

        context("file write") {

            test("inserts the entry into the destination file and prints a success message") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddCommand().test(
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
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999",
                        )

                    result.statusCode shouldBe 0
                    val content = target.readText()
                    content shouldContain "CVE-2026-9999"
                    content shouldContain "releases: [1.0.0]"
                }
            }

            test("rewrites a column-0 destination canonically so fmt --check passes") {
                val column0 =
                    """
                    schemaVersion: "1"
                    project:
                      organization: Acme Corp
                      name: Acme Web App
                      author: Acme Corp Security Team
                    releases:
                    - id: 1.0.0
                      published_at: 2026-01-15
                    vulnerabilities:
                    - id: CVE-2026-1234
                      releases: [1.0.0]
                      packages: ["pkg:npm/example-lib@2.3.0"]
                      reports:
                      - reporter: trivy
                    """.trimIndent() + "\n"
                withTempFile(prefix = "target", content = column0) { target ->
                    val result =
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --reporter trivy",
                        )

                    result.statusCode shouldBe 0
                    target.readText().split("CVE-2026-1234").size - 1 shouldBe 1

                    val check = FmtCommand().test("--check ${target.absolutePath}")
                    check.statusCode shouldBe 0
                    check.stdout shouldContain "Already formatted"
                }
            }

            test("warns when the destination contains YAML comments") {
                val commented = vulnlogYaml().replace("vulnerabilities:", "# audit notes\nvulnerabilities:")
                withTempFile(prefix = "target", content = commented) { target ->
                    val result = AddCommand().test("${target.absolutePath} --vuln-id CVE-2026-9999")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "contains YAML comments"
                    target.readText() shouldNotContain "# audit notes"
                }
            }

            test("writes the entry to multiple destinations") {
                withTempFile(prefix = "target1", content = vulnlogYaml()) { target1 ->
                    withTempFile(prefix = "target2", content = vulnlogYaml()) { target2 ->
                        val result =
                            AddCommand().test(
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
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-1234 " +
                                "--package pkg:npm/example-lib@9.9.9",
                        )

                    result.statusCode shouldBe 0
                    result.stdout shouldBe "Updated in ${target.toPath()}: CVE-2026-1234\n"

                    val content = target.readText()
                    content.split("CVE-2026-1234").size - 1 shouldBe 1
                    content shouldContain "pkg:npm/example-lib@9.9.9"
                    content shouldContain "pkg:npm/example-lib@2.3.0"
                    content shouldContain "not affected"
                    content shouldContain "Remote code execution in example-lib"
                }
            }

            test("attaches a tag that is defined in the file") {
                val withTag =
                    """
                    ---
                    schemaVersion: "1"

                    project:
                      organization: Acme Corp
                      name: Acme Web App
                      author: Acme Corp Security Team

                    tags:
                      - id: frontend
                        description: Frontend components

                    releases:
                      - id: 1.0.0
                        published_at: 2026-01-15

                    vulnerabilities: []
                    """.trimIndent()

                withTempFile(prefix = "target", content = withTag) { target ->
                    val result =
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --tag frontend",
                        )

                    result.statusCode shouldBe 0
                    val content = target.readText()
                    content shouldContain "CVE-2026-9999"
                    content shouldContain "tags: [frontend]"
                }
            }

            test("fails when --release is not defined in the file") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --release 9.9.9",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "not defined"
                }
            }

            test("fails when --tag is not defined in the file") {
                withTempFile(prefix = "target", content = vulnlogYaml()) { target ->
                    val result =
                        AddCommand().test(
                            "${target.absolutePath} --vuln-id CVE-2026-9999 --tag unknown",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "not defined"
                }
            }
        }
    })
