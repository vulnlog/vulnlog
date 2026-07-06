// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
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

        context("diagnostics") {

            test("--info shows parsed inputs and written outputs") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogSuppress", "--info").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain
                    "parsed test.vl.yaml: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                result.output shouldContain ".trivyignore.yaml: trivy format, 1 entry"
            }

            test("the default log level hides diagnostics") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogSuppress").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldNotContain "parsed test.vl.yaml"
            }

            test("--info shows entries skipped for the output format") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml(reporter = "snyk"))

                val result = runner(dir, "vulnlogSuppress", "--info").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain
                    "skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids"
            }
        }

        context("format selection") {

            test("generic format writes the generic JSON file instead of the native one") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                suppress {
                                    format = "generic"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").build()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
                val outputDir = dir.resolve("build/vulnlog/suppressions")
                val files = outputDir.list()?.toList() ?: emptyList()
                files shouldContain "trivy.generic.json"
                files.contains(".trivyignore.yaml") shouldBe false
                outputDir.resolve("trivy.generic.json").readText() shouldContain "CVE-2026-1234"
            }

            test("fails on an unknown format") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                suppress {
                                    format = "bogus"
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to vulnlogYaml(),
                    )

                val result = runner(dir, "vulnlogSuppress").buildAndFail()

                result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Unknown suppression format 'bogus'"
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
                result.output shouldContain "error: test.vl.yaml: "
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
