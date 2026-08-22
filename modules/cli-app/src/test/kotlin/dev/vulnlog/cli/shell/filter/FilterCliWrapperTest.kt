// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.filter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/** A file declaring two releases in order and one tag, for the wrapper to resolve against. */
private fun twoReleaseFile(): VulnlogFile =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")),
        tags = listOf(tagEntry("internal")),
    )

/**
 * Drives the wrapper on its own and echoes what it resolved to, so the assertions look at the exit
 * code and the messages the wrapper wrote rather than at a whole command's behaviour.
 */
private class WrapperCommand(
    private val request: FilterRequest,
    private val files: List<VulnlogFile>,
) : CliktCommand(name = "wrapper") {
    override fun run() {
        val filter = resolveFilterOrFail(request, files)
        echo("releases=${filter.releases.joinToString(",") { it.value }}", err = true)
        echo("tags=${filter.tags.joinToString(",") { it.value }}", err = true)
        echo("reporter=${filter.reporter}", err = true)
        echo("states=${filter.states.joinToString(",")}", err = true)
    }
}

private fun resolve(
    request: FilterRequest,
    files: List<VulnlogFile> = listOf(twoReleaseFile()),
) = WrapperCommand(request, files).test("")

class FilterCliWrapperTest :
    FunSpec({

        context("a filter every file knows") {

            test("hands back the expanded release window") {
                val request = FilterRequest(asOf = "2.0.0")

                val result = resolve(request)

                result.statusCode shouldBe 0
                result.stderr shouldContain "releases=1.0.0,2.0.0"
            }

            test("hands back the tags and the reporter untouched") {
                val request = FilterRequest(reporter = "trivy", tags = setOf("internal"))

                val result = resolve(request)

                result.statusCode shouldBe 0
                result.stderr shouldContain "tags=internal"
                result.stderr shouldContain "reporter=TRIVY"
            }

            test("hands back the resolved states") {
                val request = FilterRequest(states = setOf("open", "not applicable"))

                val result = resolve(request)

                result.statusCode shouldBe 0
                result.stderr shouldContain "OPEN"
                result.stderr shouldContain "NOT_APPLICABLE"
            }

            test("resolves an empty request to an inactive filter") {
                val request = FilterRequest()

                val result = resolve(request)

                result.statusCode shouldBe 0
                result.stderr shouldContain "releases="
                result.stderr shouldContain "reporter=null"
            }
        }

        context("a filter the files do not know") {

            test("fails with the invalid flag value exit code") {
                val request = FilterRequest(asOf = "9.9.9")

                val result = resolve(request)

                result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
            }

            test("reports the offending value and what the files do declare") {
                val request = FilterRequest(asOf = "9.9.9")

                val result = resolve(request)

                result.stderr shouldContain "Release not found: 9.9.9"
                result.stderr shouldContain "Known releases: 1.0.0, 2.0.0"
            }

            test("reports every failing dimension before giving up") {
                val request = FilterRequest(asOf = "9.9.9", tags = setOf("missing"))

                val result = resolve(request)

                result.stderr shouldContain "Release not found: 9.9.9"
                result.stderr shouldContain "Tag not found: missing"
            }

            test("fails with the invalid flag value exit code on an unknown state") {
                val request = FilterRequest(states = setOf("bogus"))

                val result = resolve(request)

                result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
            }

            test("reports the offending state and the states Vulnlog defines") {
                val request = FilterRequest(states = setOf("bogus"))

                val result = resolve(request)

                result.stderr shouldContain "Invalid state: bogus"
                result.stderr shouldContain
                    "Supported states: under investigation, open, accepted, resolved, not applicable"
            }

            test("keeps internals out of the message") {
                val request = FilterRequest(states = setOf("bogus"))

                val result = resolve(request)

                result.stderr shouldNotContain "dev.vulnlog"
                result.stderr shouldNotContain "No enum constant"
            }
        }
    })
