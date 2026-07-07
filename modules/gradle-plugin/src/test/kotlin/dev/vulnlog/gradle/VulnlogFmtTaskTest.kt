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

class VulnlogFmtTaskTest :
    FunSpec({

        context("formatting") {

            test("formats a file to the canonical style") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Formatted:"
                dir.resolve("test.vl.yaml").readText() shouldContain "releases: [1.0.0]"
            }

            test("is idempotent") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())

                runner(dir, "vulnlogFormat").build()
                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Unchanged: "
            }
        }

        context("--check") {

            test("fails and leaves the file untouched when it is not formatted") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())
                val original = dir.resolve("test.vl.yaml").readText()

                val result = runner(dir, "vulnlogFormat", "--check").buildAndFail()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "warning: "
                result.output shouldContain "test.vl.yaml: not canonically formatted"
                dir.resolve("test.vl.yaml").readText() shouldBe original
            }

            test("succeeds when the file is already formatted") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogYaml())
                runner(dir, "vulnlogFormat").build()

                val result = runner(dir, "vulnlogFormat", "--check").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
            }
        }

        context("input validation") {

            test("fails when no files are configured") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogFormat").buildAndFail()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No Vulnlog files configured"
            }
        }

        context("parse failures") {

            test("fails on invalid YAML") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to INVALID_VULNLOG_YAML)

                val result = runner(dir, "vulnlogFormat").buildAndFail()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "error: test.vl.yaml: "
            }
        }
    })
