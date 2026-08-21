// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.filter

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.diagnosticSink
import dev.vulnlog.cli.shell.echoMessage
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.renderFilterResolution
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity

/**
 * Checks [request] against [files] and reports what it resolved to on the verbose sink.
 * Every unknown filter value is reported before failing with [ExitCode.INVALID_FLAG_VALUE].
 */
fun CliktCommand.resolveFilterOrFail(
    request: FilterRequest,
    files: List<VulnlogFile>,
): ResolvedFilter =
    when (val outcome = resolveFilter(request, files)) {
        is FilterOutcome.Resolved -> {
            renderFilterResolution(outcome.filter).forEach { diagnosticSink().verbose(it) }
            outcome.filter
        }

        is FilterOutcome.Rejected -> {
            outcome.problems.forEach { problem ->
                echoMessage(formatMessage(FindingSeverity.ERROR, problem.message))
                echoMessage(formatHint(problem.hint))
            }
            throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
        }
    }
