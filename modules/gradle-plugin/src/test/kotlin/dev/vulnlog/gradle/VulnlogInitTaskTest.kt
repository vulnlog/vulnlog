// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

private val REQUIRED_PROPS =
    arrayOf(
        "-Pvulnlog.organization=Acme Corp",
        "-Pvulnlog.name=Widget",
        "-Pvulnlog.author=Alice",
        "-Pvulnlog.output=vulnlog.yaml",
    )

class VulnlogInitTaskTest :
    FunSpec({

        context("happy path") {

            test("creates a Vulnlog file with the configured values") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogInit", *REQUIRED_PROPS).build()

                result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.SUCCESS
                val content = dir.resolve("vulnlog.yaml").readText()
                content shouldContain "Acme Corp"
                content shouldContain "Widget"
                content shouldContain "Alice"
                content shouldContain "schemaVersion:"
            }

            test("does not overwrite an existing output file without force") {
                val dir = gradleProject(buildFile(), "vulnlog.yaml" to "keep me")

                val result = runner(dir, "vulnlogInit", *REQUIRED_PROPS).buildAndFail()

                result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "already exists"
                dir.resolve("vulnlog.yaml").readText() shouldBe "keep me"
            }

            test("overwrites an existing output file with force") {
                val dir = gradleProject(buildFile(), "vulnlog.yaml" to "replace me")

                val result = runner(dir, "vulnlogInit", *REQUIRED_PROPS, "-Pvulnlog.force=true").build()

                result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("vulnlog.yaml").readText() shouldContain "Acme Corp"
            }
        }

        context("argument validation") {

            test("fails when -Pvulnlog.organization is missing") {
                val dir = gradleProject(buildFile())

                val result =
                    runner(
                        dir,
                        "vulnlogInit",
                        "-Pvulnlog.name=Widget",
                        "-Pvulnlog.author=Alice",
                        "-Pvulnlog.output=vulnlog.yaml",
                    ).buildAndFail()

                result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.FAILED
            }

            test("fails when -Pvulnlog.output is missing") {
                val dir = gradleProject(buildFile())

                val result =
                    runner(
                        dir,
                        "vulnlogInit",
                        "-Pvulnlog.organization=Acme Corp",
                        "-Pvulnlog.name=Widget",
                        "-Pvulnlog.author=Alice",
                    ).buildAndFail()

                result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.FAILED
            }
        }
    })
