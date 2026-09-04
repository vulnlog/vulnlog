// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.vulnlogDocument
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

/** Wraps [settings] in a `vex { openvex { } }` block on a single input file. */
private fun openVexBuildFile(settings: String) =
    buildFile(
        """
        vulnlog {
            files.from("test.vl.yaml")
            vex {
                openvex {
                    $settings
                }
            }
        }
        """.trimIndent(),
    )

class VulnlogOpenVexTaskTest :
    FunSpec({

        context("happy path") {

            test("writes the document to the default output file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Wrote: "
                val document = dir.resolve("build/vulnlog/vex.json").readText()
                document shouldContain "\"@context\": \"https://openvex.dev/ns/v0.2.0\""
                document shouldContain "\"@id\": \"pkg:maven/com.acme/acme-web-app@1.0.0\""
                document shouldContain "\"status\": \"not_affected\""
            }

            test("writes the document to the configured output file") {
                val dir =
                    gradleProject(
                        openVexBuildFile("""outputFile = layout.projectDirectory.file("openvex.json")"""),
                        "test.vl.yaml" to openVexDocument(),
                    )

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("openvex.json").readText() shouldContain "\"version\": 1"
            }

            test("warns about the release without purls") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogOpenVex").build()

                result.output shouldContain "warning: releases without purls are not part of the document: '1.0.1'"
            }
        }

        context("nothing to write") {

            test("fails when no release declares purls") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogOpenVex").buildAndFail()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No statement applies."
                result.output shouldContain "Declare 'purls' on the releases"
            }
        }

        context("input validation") {

            test("fails when no Vulnlog file is configured") {
                val dir = gradleProject(buildFile())

                val result = runner(dir, "vulnlogOpenVex").buildAndFail()

                result.output shouldContain "No Vulnlog files configured"
            }

            test("fails when more than one Vulnlog file is configured") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("a.vl.yaml", "b.vl.yaml")
                            }
                            """.trimIndent(),
                        ),
                        "a.vl.yaml" to openVexDocument(),
                        "b.vl.yaml" to openVexDocument(projectName = "Other App"),
                    )

                val result = runner(dir, "vulnlogOpenVex").buildAndFail()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "vulnlogOpenVex supports a single Vulnlog file, but 2 are configured."
            }
        }

        context("up-to-date checking") {

            test("skips the task when nothing changed") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to openVexDocument())
                runner(dir, "vulnlogOpenVex").build()

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            }
        }
    })
