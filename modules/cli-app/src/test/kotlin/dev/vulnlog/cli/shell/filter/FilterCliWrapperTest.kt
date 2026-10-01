// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.filter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.cli.shell.CliDiagnostics
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.Verbosity
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val twoReleaseFile =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")),
        tags = listOf(tagEntry("internal")),
    )

private class WrapperCommand(
    private val request: FilterRequest,
) : CliktCommand(name = "wrapper") {
    var resolved: ResolvedFilter? = null

    override fun run() {
        currentContext.obj = CliDiagnostics(Verbosity(level = 1)) { message -> echo(message, err = true) }
        resolved = resolveFilterOrFail(request, listOf(twoReleaseFile))
    }
}

class FilterCliWrapperTest :
    FunSpec({

        test("hands back the resolved filter and reports it on the verbose sink") {
            val command = WrapperCommand(FilterRequest(asOf = "2.0.0", tags = setOf("internal")))

            val result = command.test("")

            result.statusCode shouldBe 0
            command.resolved shouldBe
                ResolvedFilter(releases = setOf(release("1.0.0"), release("2.0.0")), tags = setOf(tag("internal")))
            result.stderr shouldContain "as-of filter expanded to releases: 1.0.0, 2.0.0"
        }

        test("names every problem with its hint and fails with the invalid flag value exit code") {
            val result = WrapperCommand(FilterRequest(asOf = "9.9.9", tags = setOf("missing"))).test("")

            result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
            result.stderr shouldContain "error: Release not found: 9.9.9"
            result.stderr shouldContain "hint: Known releases: 1.0.0, 2.0.0"
            result.stderr shouldContain "error: Tag not found: missing"
        }
    })
