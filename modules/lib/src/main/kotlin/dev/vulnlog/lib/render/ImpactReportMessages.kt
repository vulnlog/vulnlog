// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.report.ImpactReportFilterRejected
import dev.vulnlog.lib.app.report.ImpactReportOutcome
import dev.vulnlog.lib.app.report.ImpactReportProjectsDiffer

fun renderImpactReportMessages(outcome: ImpactReportOutcome): List<Message> =
    when (outcome) {
        is ImpactReportProjectsDiffer, is ImpactReportFilterRejected -> emptyList()

        is ImpactReportOutcome.Generated -> renderFilterResolution(outcome.report.filter) + countMessage(outcome)
    }

fun renderImpactReportFailure(failed: ImpactReportOutcome.Failed): List<Failure> =
    when (failed) {
        is ImpactReportProjectsDiffer -> listOf(renderProjectsDiffer(failed))
        is ImpactReportFilterRejected -> renderFilterProblems(failed.problems)
    }

private fun countMessage(outcome: ImpactReportOutcome.Generated): Message =
    Message.Debug("collected ${outcome.collected.size} report entries, merged to ${outcome.report.entries.size}")
