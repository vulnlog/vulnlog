// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.changelogDocument
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

/** A project whose single input file exercises the changelog report, configured with [settings]. */
private fun changelogProject(settings: String) =
    gradleProject(changelogBuildFile(settings), "test.vl.yaml" to changelogDocument())

/** Wraps [settings] in a `report { changelog { } }` block on a single input file. */
private fun changelogBuildFile(settings: String) =
    buildFile(
        """
        vulnlog {
            files.from("test.vl.yaml")
            report {
                changelog {
                    $settings
                }
            }
        }
        """.trimIndent(),
    )

class VulnlogChangelogReportTaskTest :
    FunSpec({

        context("happy path") {

            test("writes the report to the default output file") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to changelogDocument())

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                result.output shouldContain "Wrote: "
                val report = dir.resolve("build/vulnlog/vulnlog-changelog.txt")
                report.readText() shouldContain "CVE-2026-1111"
            }

            test("reports the newest release first") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to changelogDocument())

                runner(dir, "vulnlogChangelogReport").build()

                val report = dir.resolve("build/vulnlog/vulnlog-changelog.txt").readText()
                report.lines().filter { it.firstOrNull()?.isDigit() == true } shouldBe
                    listOf("1.2.0 (unreleased)", "1.1.0 (2026-03-20)")
            }

            test("leaves out a vulnerability that records no resolution") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to changelogDocument())

                runner(dir, "vulnlogChangelogReport").build()

                dir.resolve("build/vulnlog/vulnlog-changelog.txt").readText() shouldNotContain "CVE-2026-3333"
            }

            test("writes the report to the configured output file") {
                val dir =
                    changelogProject("""outputFile = layout.projectDirectory.file("changelog.md")""")

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("changelog.md").exists() shouldBe true
            }

            test("merges two files of the same project into one section per release") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("a.vl.yaml", "b.vl.yaml")
                            }
                            """.trimIndent(),
                        ),
                        "a.vl.yaml" to changelogDocument(),
                        "b.vl.yaml" to changelogDocument(),
                    )

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                val report = dir.resolve("build/vulnlog/vulnlog-changelog.txt").readText()
                report.lines().count { it.startsWith("1.1.0 ") } shouldBe 1
            }
        }

        context("format") {

            test("names the default output file after the format") {
                val dir = changelogProject("""format = "markdown"""")

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("build/vulnlog/vulnlog-changelog.md").readText() shouldContain "## [1.1.0] - 2026-03-20"
            }

            test("fails on an unknown format") {
                val dir = changelogProject("""format = "yaml"""")

                val result = runner(dir, "vulnlogChangelogReport").buildAndFail()

                result.output shouldContain "Unknown changelog format 'yaml'"
            }

            test("drops the descriptions and resolution details when brief") {
                val dir = changelogProject("brief = true")

                runner(dir, "vulnlogChangelogReport").build()

                val report = dir.resolve("build/vulnlog/vulnlog-changelog.txt").readText()
                report shouldContain "CVE-2026-1111 (critical)"
                report shouldNotContain "Updated auth-middleware"
            }
        }

        context("filtering") {

            test("reports only the release the fix shipped in") {
                val dir = changelogProject("""fixedIn = "1.1.0"""")

                runner(dir, "vulnlogChangelogReport").build()

                val report = dir.resolve("build/vulnlog/vulnlog-changelog.txt").readText()
                report shouldContain "CVE-2026-1111"
                report shouldNotContain "CVE-2026-2222"
            }

            test("fails on a release the input does not declare") {
                val dir = changelogProject("""fixedIn = "9.9.9"""")

                val result = runner(dir, "vulnlogChangelogReport").buildAndFail()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "Release not found: 9.9.9"
            }
        }

        context("nothing to report") {

            test("still writes the output file when no vulnerability records a resolution") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.SUCCESS
                dir.resolve("build/vulnlog/vulnlog-changelog.txt").exists() shouldBe true
            }

            test("says that there is nothing to report") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to vulnlogDocument())

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.output shouldContain "info: no fixed vulnerabilities to report"
            }
        }

        context("up-to-date checking") {

            test("skips the task when nothing changed") {
                val dir = gradleProject(FILES_FROM_TEST_YAML, "test.vl.yaml" to changelogDocument())
                runner(dir, "vulnlogChangelogReport").build()

                val result = runner(dir, "vulnlogChangelogReport").build()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.UP_TO_DATE
            }
        }

        context("merge validation") {

            test("fails when input files have different project metadata") {
                val dir =
                    gradleProject(
                        buildFile(
                            """
                            vulnlog {
                                files.from("a.vl.yaml", "b.vl.yaml")
                            }
                            """.trimIndent(),
                        ),
                        "a.vl.yaml" to changelogDocument(),
                        "b.vl.yaml" to changelogDocument(projectName = "Other App"),
                    )

                val result = runner(dir, "vulnlogChangelogReport").buildAndFail()

                result.task(":vulnlogChangelogReport")?.outcome shouldBe TaskOutcome.FAILED
                result.output shouldContain "must share the same project metadata"
            }
        }
    })
