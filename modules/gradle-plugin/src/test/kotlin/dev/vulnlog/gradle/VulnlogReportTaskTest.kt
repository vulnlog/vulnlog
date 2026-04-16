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

private val OTHER_PROJECT_VULNLOG_YAML =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Other Corp
      name: Other App
      author: Other Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:
      - id: CVE-2026-9999
        releases: [ 1.0.0 ]
        description: Other vuln
        packages: [ "pkg:npm/other-lib@1.0.0" ]
        reports:
          - reporter: trivy
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
    val dir = createTempDirectory("vulnlog-report-test").toFile()
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

class VulnlogReportTaskTest :
    FunSpec({

        test("writes report to default output file") {
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

            val result = runner(dir, "vulnlogReport").build()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Report written to:"
            val report = dir.resolve("build/vulnlog/vulnlog-report.html")
            report.exists() shouldBe true
            report.readText() shouldContain "CVE-2026-1234"
        }

        test("writes report to configured output file") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                            report {
                                outputFile = layout.projectDirectory.file("custom-report.html")
                            }
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to VALID_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogReport").build()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
            dir.resolve("custom-report.html").exists() shouldBe true
        }

        test("merges entries from multiple files of the same project") {
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

            val result = runner(dir, "vulnlogReport").build()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.SUCCESS
            dir.resolve("build/vulnlog/vulnlog-report.html").exists() shouldBe true
        }

        test("fails when input files have different project metadata") {
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
                    "b.vl.yaml" to OTHER_PROJECT_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogReport").buildAndFail()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "must share the same project metadata"
        }

        test("fails when no files are configured") {
            val dir = projectDir(buildFile())

            val result = runner(dir, "vulnlogReport").buildAndFail()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "No Vulnlog files configured"
        }

        test("fails on invalid reporter value") {
            val dir =
                projectDir(
                    buildFile(
                        """
                        vulnlog {
                            files.from("test.vl.yaml")
                            report {
                                reporter = "bogus"
                            }
                        }
                        """.trimIndent(),
                    ),
                    "test.vl.yaml" to VALID_VULNLOG_YAML,
                )

            val result = runner(dir, "vulnlogReport").buildAndFail()

            result.task(":vulnlogReport")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Invalid reporter: bogus"
        }
    })
