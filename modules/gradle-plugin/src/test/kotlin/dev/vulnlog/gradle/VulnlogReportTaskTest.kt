// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

private val FILES_FROM_TEST_YAML =
    buildFile(
        """
        vulnlog {
            files.from("test.vl.yaml")
        }
        """.trimIndent(),
    )

class VulnlogReportTaskTest :
    FunSpec({

        context("happy path") {

            test("writes the report to the default output file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Report written to:"
                val report = dir.resolve("build/vulnlog/vulnlog-report.html")
                report.exists() shouldBe true
                report.readText() shouldContain "CVE-2026-1234"
            }

            test("writes the report to the configured output file") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                report {
                                    outputFile = layout.projectDirectory.file("custom-report.html")
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("custom-report.html").exists() shouldBe true
            }

            test("merges entries from multiple files of the same project") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("a.vl.yaml", "b.vl.yaml")
                            }
                            """.trimIndent(),
                        ),
                        "a.vl.yaml" to vulnlogYaml(),
                        "b.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("build/vulnlog/vulnlog-report.html").exists() shouldBe true
            }
        }

        context("input validation") {

            test("fails when no files are configured") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No Vulnlog files configured"
            }
        }

        context("parse failures") {

            test("fails on invalid YAML") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to INVALID_VULNLOG_YAML)

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Parsing of test.vl.yaml failed"
            }
        }

        context("merge validation") {

            test("fails when input files have different project metadata") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("a.vl.yaml", "b.vl.yaml")
                            }
                            """.trimIndent(),
                        ),
                        "a.vl.yaml" to vulnlogYaml(projectName = "Acme Web App"),
                        "b.vl.yaml" to vulnlogYaml(projectName = "Other App", organization = "Other Corp"),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "must share the same project metadata"
            }
        }

        context("filter validation") {

            test("fails on an unknown reporter") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                report {
                                    reporter = "bogus"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Invalid reporter: bogus"
            }

            test("fails on an unknown release") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                report {
                                    release = "9.9.9"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Release not found: 9.9.9"
                result.output shouldContain "Known releases: 1.0.0"
            }

            test("fails on an unknown tag") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                report {
                                    tags = setOf("missing-tag")
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Tag not found: missing-tag"
            }
        }
    })
