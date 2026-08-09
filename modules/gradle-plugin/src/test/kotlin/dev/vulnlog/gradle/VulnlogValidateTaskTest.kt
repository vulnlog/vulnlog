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

class VulnlogValidateTaskTest :
    FunSpec({

        context("happy path") {

            test("succeeds on a valid Vulnlog file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Validated: test.vl.yaml"
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
                        "a.vl.yaml" to vulnlogDocument(),
                        "b.vl.yaml" to vulnlogDocument(),
                    )

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Validated: a.vl.yaml"
                result.output shouldContain "Validated: b.vl.yaml"
            }

            test("prints INFO-level findings for files with informational observations") {
                val dir =
                    gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to ValidationDocuments.UNREFERENCED_RELEASE)

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "info: test.vl.yaml: "
                result.output shouldContain "Unreferenced release ID"
                result.output shouldContain "Validated: test.vl.yaml"
            }
        }

        context("diagnostics") {

            test("--info shows the parsed input and the per-file validation summary") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogValidate", "--info").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain
                    "parsed test.vl.yaml: schema version 1, releases: 1, tags: 0, vulnerabilities: 1"
                result.output shouldContain "validated test.vl.yaml: no findings"
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
                result.output shouldContain "error: test.vl.yaml: "
            }
        }

        context("warnings") {

            test("succeeds with warnings when strict is false") {
                val dir =
                    gradleProject(
                        FILES_FROM_TEST_YAML,
                        "test.vl.yaml" to ValidationDocuments.ANALYZED_BEFORE_REPORTED,
                    )

                val result = runner(dir, "vulnlogValidate").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "warning: test.vl.yaml: "
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
                        "test.vl.yaml" to ValidationDocuments.ANALYZED_BEFORE_REPORTED,
                    )

                val result = runner(dir, "vulnlogValidate").buildAndFail()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Vulnlog validation failed"
            }
        }

        context("native log levels") {

            test("errors are logged at the error level and survive --quiet") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to INVALID_VULNLOG_YAML)

                val result = runner(dir, "vulnlogValidate", "--quiet").buildAndFail()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "error: test.vl.yaml: "
            }

            test("warnings are logged above the lifecycle status") {
                val dir =
                    gradleProject(
                        FILES_FROM_TEST_YAML,
                        "test.vl.yaml" to ValidationDocuments.ANALYZED_BEFORE_REPORTED,
                    )

                val result = runner(dir, "vulnlogValidate", "--warn").build()

                result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "warning: test.vl.yaml: "
                result.output shouldNotContain "Validated: test.vl.yaml"
            }
        }
    })
