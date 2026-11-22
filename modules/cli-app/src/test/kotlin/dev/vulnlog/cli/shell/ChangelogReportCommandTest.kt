// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.changelogDocument
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

class ChangelogReportCommandTest :
    FunSpec({

        context("happy path") {

            test("writes the changelog to stdout") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "Acme Corp / Acme Web App"
                    result.stdout shouldContain "CVE-2026-1111 (critical) Authentication bypass in auth-middleware"
                }
            }

            test("writes the report to the given path") {
                withTempFile(content = changelogDocument()) { input ->
                    withTempFile(prefix = "changelog", suffix = ".md") { output ->
                        val result =
                            ChangelogReportCommand().test("${input.absolutePath} -o ${output.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Wrote: "
                        output.readText() shouldContain "CVE-2026-1111"
                    }
                }
            }

            test("merges two files of the same project into one section per release") {
                withTempFile(prefix = "vulnlog-a", content = changelogDocument()) { first ->
                    withTempFile(prefix = "vulnlog-b", content = changelogDocument()) { second ->
                        val result =
                            ChangelogReportCommand().test("${first.absolutePath} ${second.absolutePath}")

                        result.statusCode shouldBe 0
                        result.stdout.lines().count { it.startsWith("1.1.0 ") } shouldBe 1
                    }
                }
            }
        }

        context("format") {

            test("renders markdown sections on request") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --format markdown")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "## [1.1.0] - 2026-03-20"
                    result.stdout shouldContain "### Security"
                }
            }

            test("writes JSON with every field, also when brief") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --format json --brief")

                    result.statusCode shouldBe 0
                    result.stdout shouldStartWith "{"
                    result.stdout shouldContain "\"primary_id\" : \"CVE-2026-1111\""
                    result.stdout shouldContain "Updated auth-middleware"
                }
            }

            test("names json and the reach of brief in the help text") {
                val result = ChangelogReportCommand().test("--help")

                unwrapped(result.stdout) shouldContain "'json' holds every field and the metadata of the run"
                unwrapped(result.stdout) shouldContain "Applies to text and markdown. JSON always holds every field."
            }

            test("rejects an unknown format") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --format yaml")

                    result.statusCode shouldBe 1
                }
            }

            test("drops the descriptions and resolution details when brief") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --brief")

                    result.stdout shouldContain "CVE-2026-1111 (critical)"
                    result.stdout shouldNotContain "Updated auth-middleware"
                }
            }
        }

        context("filtering") {

            test("reports only the release the fix shipped in") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --fixed-in 1.1.0")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "CVE-2026-1111"
                    result.stdout shouldNotContain "CVE-2026-2222"
                }
            }

            test("leaves out a fix that ships after the requested release") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --as-of 1.1.0")

                    result.statusCode shouldBe 0
                    result.stdout shouldNotContain "CVE-2026-2222"
                }
            }

            test("lists the known releases for a release the input does not declare") {
                withTempFile(content = changelogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --fixed-in 9.9.9")

                    result.statusCode shouldBe 5
                    result.stderr shouldContain "Release not found: 9.9.9"
                    result.stderr shouldContain "Known releases: 1.0.0, 1.1.0, 1.2.0"
                }
            }
        }

        context("failure") {

            test("reports nothing to do when no vulnerability records a resolution") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ChangelogReportCommand().test(input.absolutePath)

                    result.statusCode shouldBe 0
                    result.stdout shouldBe ""
                    result.stderr shouldContain "info: no fixed vulnerabilities to report"
                }
            }

            test("still writes a JSON document without releases to stdout when no vulnerability records a resolution") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = ChangelogReportCommand().test("${input.absolutePath} --format json")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "\"releases\" : [ ]"
                    result.stderr shouldContain "info: no fixed vulnerabilities to report"
                }
            }

            test("still writes a JSON document without releases to the output file when nothing was fixed") {
                withTempFile(content = vulnlogDocument()) { input ->
                    withTempFile(prefix = "changelog", suffix = ".json") { output ->
                        val result =
                            ChangelogReportCommand().test(
                                "${input.absolutePath} --format json -o ${output.absolutePath}",
                            )

                        result.statusCode shouldBe 0
                        output.readText() shouldContain "\"releases\" : [ ]"
                    }
                }
            }

            test("refuses to merge files that describe different projects") {
                withTempFile(prefix = "vulnlog-a", content = changelogDocument()) { first ->
                    withTempFile(
                        prefix = "vulnlog-b",
                        content = changelogDocument(projectName = "Other App"),
                    ) { second ->
                        val result =
                            ChangelogReportCommand().test("${first.absolutePath} ${second.absolutePath}")

                        result.statusCode shouldBe 2
                        result.stderr shouldContain "must share the same project metadata"
                    }
                }
            }
        }
    })
