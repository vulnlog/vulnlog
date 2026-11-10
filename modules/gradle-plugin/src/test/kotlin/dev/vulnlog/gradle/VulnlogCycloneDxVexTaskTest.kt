// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.openVexScopedDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.TaskOutcome

private fun cycloneDxBuildFile(settings: String) =
    buildFile(
        """
        vulnlog {
            files.from("test.vl.yaml")
            vex {
                cyclonedx {
                    $settings
                }
            }
        }
        """.trimIndent(),
    )

private val RELEASE_1_0_0 = cycloneDxBuildFile("""release = "1.0.0"""")

class VulnlogCycloneDxVexTaskTest :
    FunSpec({

        context("happy path") {

            test("writes the document for the release to the default output file") {
                val dir = gradleProject(RELEASE_1_0_0, "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogCycloneDxVex").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Wrote: "
                val document = dir.resolve("build/vulnlog/vex.cdx.json").readText()
                document shouldContain "\"bomFormat\": \"CycloneDX\""
                document shouldContain "\"specVersion\": \"1.7\""
                document shouldContain "\"purl\": \"pkg:maven/com.acme/acme-web-app@1.0.0\""
                document shouldContain "\"state\": \"not_affected\""
            }

            test("writes the document to the configured output file") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            outputFile = layout.projectDirectory.file("cyclonedx.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexDocument(),
                    )

                val result = runner(dir, "vulnlogCycloneDxVex").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("cyclonedx.json").readText() shouldContain "\"version\": 1"
            }
        }

        context("input validation") {

            test("fails when no release is configured, because a document covers one release") {
                val dir = gradleProject(cycloneDxBuildFile(""), "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.output shouldContain "property 'release' doesn't have a configured value"
            }

            test("fails when no Vulnlog file is configured") {
                val dir = gradleProject(buildFile("""vulnlog { vex { cyclonedx { release = "1.0.0" } } }"""))

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.output shouldContain "No Vulnlog files configured"
            }
        }

        context("nothing to write") {

            test("fails when the release declares no purls") {
                val dir =
                    gradleProject(cycloneDxBuildFile("""release = "1.0.1""""), "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "No vulnerability applies."
                result.output shouldContain "Declare 'purls' on release '1.0.1'."
            }
        }

        context("scoping") {

            test("tags keep only the vulnerabilities and release purls carrying one of them") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            tags = setOf("container")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )

                val result = runner(dir, "vulnlogCycloneDxVex").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                val document = dir.resolve("build/vulnlog/vex.cdx.json").readText()
                document shouldContain "pkg:docker/acme/web-app@1.0.0"
                document shouldNotContain "pkg:maven/com.acme/acme-lib@1.0.0"
            }

            test("fails on an unknown release") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile("""release = "9.9.9""""),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.output shouldContain "Release not found: 9.9.9"
            }
        }

        context("baseline") {

            test("keeps the serial number and counts the version up when the content changed") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            baseline = layout.projectDirectory.file("previous.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )
                dir.resolve("previous.json").writeText(
                    """
                    {
                      "bomFormat": "CycloneDX",
                      "specVersion": "1.7",
                      "serialNumber": "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79",
                      "version": 7,
                      "metadata": { "timestamp": "2026-04-25T00:00:00Z" },
                      "vulnerabilities": []
                    }
                    """.trimIndent(),
                )

                val result = runner(dir, "vulnlogCycloneDxVex").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                val document = dir.resolve("build/vulnlog/vex.cdx.json").readText()
                document shouldContain "\"serialNumber\": \"urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79\""
                document shouldNotContain "2026-04-25T00:00:00Z"
                document shouldContain "\"version\": 8"
                document shouldContain "\"name\": \"Vulnlog Gradle plugin\""
            }

            test("fails when the baseline is not a CycloneDX document") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            baseline = layout.projectDirectory.file("previous.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexDocument(),
                    )
                dir.resolve("previous.json").writeText("""{"@context": "https://openvex.dev/ns/v0.2.0"}""")

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.output shouldContain "previous.json' is not a CycloneDX document"
                result.output shouldContain "Omit 'baseline' to issue a new document."
            }

            test("fails when the baseline is the output file") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            outputFile = layout.projectDirectory.file("vex.cdx.json")
                            baseline = layout.projectDirectory.file("vex.cdx.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexScopedDocument(),
                    )
                dir.resolve("vex.cdx.json").writeText("{}")

                val result = runner(dir, "vulnlogCycloneDxVex").buildAndFail()

                result.output shouldContain
                    "Keep outputFile under the build directory and run 'vulnlogCycloneDxVexUpdate'"
            }
        }

        context("update") {

            test("creates the baseline from the generated document") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            baseline = layout.projectDirectory.file("vex/app.cdx.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexDocument(),
                    )

                val result = runner(dir, "vulnlogCycloneDxVexUpdate").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                result.task(":vulnlogCycloneDxVexUpdate")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("vex/app.cdx.json").readText() shouldBe dir.resolve("build/vulnlog/vex.cdx.json").readText()
            }

            test("settles after one update because an unchanged document keeps the baseline bytes") {
                val dir =
                    gradleProject(
                        cycloneDxBuildFile(
                            """
                            release = "1.0.0"
                            baseline = layout.projectDirectory.file("vex.cdx.json")
                            """.trimIndent(),
                        ),
                        "test.vl.yaml" to openVexDocument(),
                    )
                runner(dir, "vulnlogCycloneDxVexUpdate", "--configuration-cache").build()
                val committed = dir.resolve("vex.cdx.json").readText()

                val second = runner(dir, "vulnlogCycloneDxVexUpdate", "--configuration-cache").build()
                val third = runner(dir, "vulnlogCycloneDxVexUpdate", "--configuration-cache").build()

                second.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.SUCCESS
                second.output shouldContain "Unchanged: "
                second.task(":vulnlogCycloneDxVexUpdate")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                third.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                third.task(":vulnlogCycloneDxVexUpdate")?.outcome shouldBe TaskOutcome.UP_TO_DATE
                dir.resolve("vex.cdx.json").readText() shouldBe committed
            }

            test("fails when no baseline is configured") {
                val dir = gradleProject(RELEASE_1_0_0, "test.vl.yaml" to openVexDocument())

                val result = runner(dir, "vulnlogCycloneDxVexUpdate").buildAndFail()

                result.output shouldContain "property 'baseline' doesn't have a configured value"
            }
        }

        context("up-to-date checking") {

            test("skips the task when nothing changed") {
                val dir = gradleProject(RELEASE_1_0_0, "test.vl.yaml" to openVexDocument())
                runner(dir, "vulnlogCycloneDxVex").build()

                val result = runner(dir, "vulnlogCycloneDxVex").build()

                result.task(":vulnlogCycloneDxVex")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            }
        }
    })
