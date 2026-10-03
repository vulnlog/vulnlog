// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.finding.FindingSeverity

fun renderChangelogMessages(outcome: ChangelogOutcome): List<Message> =
    when (outcome) {
        is ProjectsDiffer, is FilterRejected -> emptyList()

        is ChangelogOutcome.Fixed -> generatedMessages(outcome)

        is ChangelogOutcome.NothingFixed ->
            generatedMessages(outcome) +
                Message.Status(formatMessage(FindingSeverity.INFO, "no fixed vulnerabilities to report"))
    }

fun renderChangelogFailure(failed: ChangelogOutcome.Failed): List<Failure> =
    when (failed) {
        is ProjectsDiffer -> listOf(renderProjectsDiffer(failed))
        is FilterRejected -> renderFilterProblems(failed.problems)
    }

private fun generatedMessages(outcome: ChangelogOutcome.Generated): List<Message> {
    val releases = outcome.changelog.releases
    val fixes = releases.sumOf { it.entries.size }
    return renderFilterResolution(outcome.filter) + Message.Debug("collected $fixes fixes in ${releases.size} releases")
}
