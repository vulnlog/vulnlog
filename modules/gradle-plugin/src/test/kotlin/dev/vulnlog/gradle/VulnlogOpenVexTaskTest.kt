// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.openVexScopedDocument
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

        context("scoping") {

            test("asOf covers the named release and every earlier one") {
                val dir =
                    gradleProject(
                        openVexBuildFile("""asOf = "1.1.0""""),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                val document = dir.resolve("build/vulnlog/vex.json").readText()
                document shouldContain "pkg:docker/acme/web-app@1.0.0"
                document shouldContain "pkg:docker/acme/web-app@1.1.0"
            }

            test("tags keep only the purls carrying one of them") {
                val dir =
                    gradleProject(
                        openVexBuildFile("""tags = setOf("container")"""),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                val document = dir.resolve("build/vulnlog/vex.json").readText()
                document shouldContain "pkg:docker/acme/web-app@1.0.0"
                document shouldNotContain "pkg:maven/com.acme/acme-lib@1.0.0"
            }

            test("fails on an unknown tag") {
                val dir =
                    gradleProject(
                        openVexBuildFile("""tags = setOf("binary")"""),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )

                val result = runner(dir, "vulnlogOpenVex").buildAndFail()

                result.output shouldContain "Tag not found: binary"
            }
        }

        context("baseline") {

            test("keeps the identifier and counts the version up when the content changed") {
                val dir =
                    gradleProject(
                        openVexBuildFile(
                            """
                            baseline = layout.projectDirectory.file("previous.json")
                            tags = setOf("container")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )
                val previous = dir.resolve("previous.json")
                previous.writeText(
                    """
                    {
                      "@context": "https://openvex.dev/ns/v0.2.0",
                      "@id": "https://vulnlog.dev/vex/kept-across-runs",
                      "author": "Acme Corp Security Team (security@acme.example)",
                      "timestamp": "2026-04-25T00:00:00Z",
                      "version": 7,
                      "statements": []
                    }
                    """.trimIndent(),
                )

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                val document = dir.resolve("build/vulnlog/vex.json").readText()
                document shouldContain "\"@id\": \"https://vulnlog.dev/vex/kept-across-runs\""
                document shouldContain "\"timestamp\": \"2026-04-25T00:00:00Z\""
                document shouldContain "\"version\": 8"
            }

            test("writes the baseline bytes back and reports it when nothing changed") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to openVexDocument())
                runner(dir, "vulnlogOpenVex").build()
                val previous = dir.resolve("previous.json")
                previous.writeText(dir.resolve("build/vulnlog/vex.json").readText())
                dir
                    .resolve("build.gradle.kts")
                    .writeText(openVexBuildFile("""baseline = layout.projectDirectory.file("previous.json")"""))

                val result = runner(dir, "vulnlogOpenVex").build()

                result.task(":vulnlogOpenVex")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Unchanged: "
                dir.resolve("build/vulnlog/vex.json").readText() shouldBe previous.readText()
            }

            test("fails when the baseline is the output file") {
                val dir =
                    gradleProject(
                        openVexBuildFile(
                            """
                            outputFile = layout.projectDirectory.file("vex.json")
                            baseline = layout.projectDirectory.file("vex.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )
                dir.resolve("vex.json").writeText("{}")

                val result = runner(dir, "vulnlogOpenVex").buildAndFail()

                result.output shouldContain "Gradle cannot read and write one file in a single task"
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
