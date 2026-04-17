// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

private val VALID_VULNLOG_YAML =
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
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

private val INVALID_VULNLOG_YAML =
    """
    ---
    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team
    """.trimIndent()

private val WARNING_VULNLOG_YAML =
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
        analyzed_at: 2025-01-01
        reports:
          - reporter: trivy
            at: 2026-06-01
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
    val dir = createTempDirectory("vulnlog-test").toFile()
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

class VulnlogValidateTaskTest :
    FunSpec({

        test("succeeds on valid vulnlog file") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to VALID_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogValidate").build()

            result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Vulnlog validation OK"
        }

        test("fails when no files are configured") {
            val dir = projectDir(buildFile())

            val result = runner(dir, "vulnlogValidate").buildAndFail()

            result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "No Vulnlog files configured"
        }

        test("fails on invalid yaml") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to INVALID_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogValidate").buildAndFail()

            result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Parsing"
            result.output shouldContain "failed"
        }

        test("succeeds with warnings when strict is false") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to WARNING_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogValidate").build()

            result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "WARN"
        }

        test("fails with warnings when strict is true") {
            val dir =
                projectDir(
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

        test("validates multiple files") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("a.vl.yaml", "b.vl.yaml")
                        }
                        """.trimIndent(),
                    ),
                    "a.vl.yaml" to VALID_VULNLOG_YAML,
                    "b.vl.yaml" to VALID_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogValidate").build()

            result.task(":vulnlogValidate")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Vulnlog validation OK"
        }
    })
