// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Wrote: "
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
                        "test.vl.yaml" to vulnlogDocument(),
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
                        "a.vl.yaml" to vulnlogDocument(),
                        "b.vl.yaml" to vulnlogDocument(),
                    )

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("build/vulnlog/vulnlog-report.html").exists() shouldBe true
            }
        }

        context("diagnostics") {

            test("--info shows the parsed inputs and the written output") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogReport", "--info").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain
                    "parsed test.vl.yaml: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                result.output shouldContain "wrote "
            }

            test("does not print INFO-level validation findings") {
                val dir =
                    gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to ValidationDocuments.UNREFERENCED_RELEASE)

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldNotContain "info: test.vl.yaml: "
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
                result.output shouldContain "error: test.vl.yaml: "
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
                        "a.vl.yaml" to vulnlogDocument(projectName = "Acme Web App"),
                        "b.vl.yaml" to vulnlogDocument(projectName = "Other App", organization = "Other Corp"),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "must share the same project metadata"
            }
        }

        context("filter validation") {

            test("a known reporter selects the entries it reported") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                report {
                                    reporter = "trivy"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogDocument(reporter = "trivy"),
                    )

                val result = runner(dir, "vulnlogReport").build()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("build/vulnlog/vulnlog-report.html").readText() shouldContain "CVE-2026-1234"
            }

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
                        "test.vl.yaml" to vulnlogDocument(),
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
                        "test.vl.yaml" to vulnlogDocument(),
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
                        "test.vl.yaml" to vulnlogDocument(),
                    )

                val result = runner(dir, "vulnlogReport").buildAndFail()

                result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Tag not found: missing-tag"
            }
        }
    })
