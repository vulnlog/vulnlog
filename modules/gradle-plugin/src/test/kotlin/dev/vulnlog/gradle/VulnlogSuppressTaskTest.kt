// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
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

class VulnlogSuppressTaskTest :
    FunSpec({

        context("happy path") {

            test("writes a suppression file to the default output directory") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogSuppress").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Suppression file created at:"
                val outputDir = dir.resolve("build/vulnlog/suppressions")
                (outputDir.list()?.toList() ?: emptyList()) shouldContain ".trivyignore.yaml"
                outputDir.resolve(".trivyignore.yaml").readText() shouldContain "CVE-2026-1234"
            }

            test("writes a suppression file to the configured output directory") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                suppress {
                                    outputDir = layout.projectDirectory.dir("out")
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("out/.trivyignore.yaml").exists() shouldBe true
            }

            test("filters by reporter") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                suppress {
                                    reporter = "trivy"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to MULTI_REPORTER_VULNLOG_YAML,
                    )

                val result = runner(dir, "vulnlogSuppress").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                val outputDir = dir.resolve("build/vulnlog/suppressions")
                val files = outputDir.list()?.toList() ?: emptyList()
                files shouldContain ".trivyignore.yaml"
                files.any { it.startsWith("grype") } shouldBe false
            }
        }

        context("input validation") {

            test("fails when no files are configured") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No Vulnlog files configured"
            }

            test("fails when multiple files are configured") {
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

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "supports a single Vulnlog file"
            }
        }

        context("parse failures") {

            test("fails on invalid YAML") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to INVALID_VULNLOG_YAML)

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Parsing of test.vl.yaml failed"
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
                                suppress {
                                    reporter = "bogus"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Invalid reporter: bogus"
            }

            test("fails on an unknown release") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                suppress {
                                    release = "9.9.9"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
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
                                suppress {
                                    tags = setOf("missing-tag")
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Tag not found: missing-tag"
            }
        }
    })
