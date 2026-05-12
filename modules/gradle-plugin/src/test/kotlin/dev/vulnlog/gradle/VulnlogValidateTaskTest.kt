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

class VulnlogValidateTaskTest :
    FunSpec({

        context("happy path") {

            test("succeeds on a valid Vulnlog file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Vulnlog validation OK"
            }

            test("succeeds on multiple valid files") {
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

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Vulnlog validation OK"
            }
        }

        context("input validation") {

            test("fails when no files are configured") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogValidate").buildAndFail()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No Vulnlog files configured"
            }
        }

        context("parse failures") {

            test("fails on invalid YAML") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to INVALID_VULNLOG_YAML)

                val result = runner(dir, "vulnlogValidate").buildAndFail()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Parsing of test.vl.yaml failed"
            }
        }

        context("warnings") {

            test("succeeds with warnings when strict is false") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to WARNING_VULNLOG_YAML)

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "WARN"
            }

            test("fails with warnings when strict is true") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("test.vl.yaml")
                                validate {
                                    strict = true
                                }
                            }
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to WARNING_VULNLOG_YAML,
                    )

                val result = runner(dir, "vulnlogValidate").buildAndFail()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Vulnlog validation failed"
            }
        }
    })
