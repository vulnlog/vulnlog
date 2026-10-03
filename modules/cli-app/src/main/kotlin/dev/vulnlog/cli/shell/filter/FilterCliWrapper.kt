// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.filter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.echoMessage
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.render.formatFailureLines
import dev.vulnlog.lib.render.renderFilterProblems
import dev.vulnlog.lib.render.renderFilterResolution

/** Also reports what [request] resolved to, at verbose level. */
fun CliktCommand.resolveFilterOrFail(
    request: FilterRequest,
    files: List<VulnlogFile>,
): ResolvedFilter =
    when (val outcome = resolveFilter(request, files)) {
        is FilterOutcome.Resolved -> {
            renderFilterResolution(outcome.filter).forEach(::echoMessage)
            outcome.filter
        }

        is FilterOutcome.Rejected -> failOnFilterProblems(outcome.problems)
    }

private fun CliktCommand.failOnFilterProblems(problems: List<FilterProblem>): Nothing {
    formatFailureLines(renderFilterProblems(problems)).forEach(::echoMessage)
    throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
}
