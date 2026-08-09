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

class VulnlogFmtTaskTest :
    FunSpec({

        context("formatting") {

            test("formats a file to the canonical style") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Formatted:"
                dir.resolve("test.vl.yaml").readText() shouldContain "releases: [1.0.0]"
            }

            test("is idempotent") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                runner(dir, "vulnlogFormat").build()
                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Unchanged: "
            }

            test("formats a file that parses but would fail validation") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to ValidationDocuments.DANGLING_RELEASE)

                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Formatted:"
                dir.resolve("test.vl.yaml").readText() shouldContain "releases: [9.9.9]"
            }

            test("removes comments and warns about it") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to COMMENTED_VULNLOG_YAML)

                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "contains YAML comments"
                dir.resolve("test.vl.yaml").readText() shouldNotContain "# audit notes"
            }
        }

        context("--check") {

            test("fails and leaves the file untouched when it is not formatted") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())
                val original = dir.resolve("test.vl.yaml").readText()

                val result = runner(dir, "vulnlogFormat", "--check").buildAndFail()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "warning: "
                result.output shouldContain "test.vl.yaml: not canonically formatted"
                dir.resolve("test.vl.yaml").readText() shouldBe original
            }

            test("succeeds when the file is already formatted") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())
                runner(dir, "vulnlogFormat").build()

                val result = runner(dir, "vulnlogFormat", "--check").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
            }

            test("checks a file that parses but would fail validation") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to ValidationDocuments.DANGLING_RELEASE)

                val result = runner(dir, "vulnlogFormat", "--check").buildAndFail()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "test.vl.yaml: not canonically formatted"
            }
        }

        context("diagnostics") {

            test("--info reports the rewritten file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogFormat", "--info").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "wrote ${dir.resolve("test.vl.yaml")}"
            }

            test("the default log level hides diagnostics") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogFormat").build()

                result.task(":vulnlogFormat")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldNotContain "wrote "
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
