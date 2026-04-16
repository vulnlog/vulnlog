// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

private val SUPPRESSABLE_VULNLOG_YAML =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:
      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: vulnerable code not in execute path
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

private val MULTI_REPORTER_VULNLOG_YAML =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:
      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
          - reporter: grype
        analysis: vulnerable code not in execute path
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

private fun buildFile(extra: String = "") =
    """
    plugins {
        id("dev.vulnlog.plugin")
    }
    $extra
    """.trimIndent()

private fun projectDir(
    buildScript: String,
    vararg yamlFiles: Pair<String, String>,
): File {
    val dir = createTempDirectory("vulnlog-suppress-test").toFile()
    dir.resolve("build.gradle.kts").writeText(buildScript)
    dir.resolve("settings.gradle.kts").writeText("")
    for ((name, content) in yamlFiles) {
        dir.resolve(name).writeText(content)
    }
    return dir
}

private fun runner(
    projectDir: File,
    vararg args: String,
): GradleRunner =
    GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)

class VulnlogSuppressTaskTest :
    FunSpec({

        test("writes suppression file to default output directory") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to SUPPRESSABLE_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogSuppress").build()

            result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Suppression file created at:"
            val outputDir = dir.resolve("build/vulnlog/suppressions")
            (outputDir.list()?.toList() ?: emptyList()) shouldContain ".trivyignore.yaml"
            outputDir.resolve(".trivyignore.yaml").readText() shouldContain "CVE-2026-1234"
        }

        test("writes suppression file to configured output directory") {
            val dir =
                projectDir(
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
                    "test.vl.yaml" to SUPPRESSABLE_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogSuppress").build()

            result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.SUCCESS
            dir.resolve("out/.trivyignore.yaml").exists() shouldBe true
        }

        test("filters by reporter") {
            val dir =
                projectDir(
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

        test("fails when no files are configured") {
            val dir = projectDir(buildFile())

            val result = runner(dir, "vulnlogSuppress").buildAndFail()

            result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "No Vulnlog files configured"
        }

        test("fails when multiple files are configured") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("a.vl.yaml", "b.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "a.vl.yaml" to SUPPRESSABLE_VULNLOG_YAML,
                    "b.vl.yaml" to SUPPRESSABLE_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogSuppress").buildAndFail()

            result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "supports a single Vulnlog file"
        }

        test("fails on invalid reporter value") {
            val dir =
                projectDir(
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
                    "test.vl.yaml" to SUPPRESSABLE_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogSuppress").buildAndFail()

            result.task(":vulnlogSuppress")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Invalid reporter: bogus"
        }
    })
