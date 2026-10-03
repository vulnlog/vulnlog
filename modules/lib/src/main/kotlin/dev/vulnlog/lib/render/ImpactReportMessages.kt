// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.ProjectsDiffer

fun renderImpactReportMessages(outcome: ImpactReportOutcome): List<Message> =
    when (outcome) {
        is ProjectsDiffer, is FilterRejected -> emptyList()

        is ImpactReportOutcome.Generated -> renderFilterResolution(outcome.report.filter) + countMessage(outcome)
    }

fun renderImpactReportFailure(failed: ImpactReportOutcome.Failed): List<Failure> =
    when (failed) {
        is ProjectsDiffer -> listOf(renderProjectsDiffer(failed))
        is FilterRejected -> renderFilterProblems(failed.problems)
    }

fun renderImpactReportWritten(target: String): Message = Message.Verbose("wrote $target")

private fun countMessage(outcome: ImpactReportOutcome.Generated): Message =
    Message.Debug("collected ${outcome.collected.size} report entries, merged to ${outcome.report.entries.size}")
