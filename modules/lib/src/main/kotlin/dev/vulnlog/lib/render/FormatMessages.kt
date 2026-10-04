// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FormatOutcome
import dev.vulnlog.lib.finding.FormatFinding

/**
 * A file that is not canonical carries its findings inside the warning: they explain it, and [Message] has no level
 * for a line that is always shown but is no warning.
 */
fun renderFormatMessages(outcome: FormatOutcome): List<Message> =
    when (outcome) {
        is FormatOutcome.Unchanged -> emptyList()

        is FormatOutcome.Reformatted ->
            listOfNotNull(renderCommentsDropped(outcome.document.source).takeIf { outcome.commentsDropped }) +
                outcome.findings.map { finding -> Message.Debug(renderFormatFinding(finding)) }

        is FormatOutcome.NotCanonical -> {
            val findings = outcome.findings.map { finding -> "\n  ${renderFormatFinding(finding)}" }
            listOf(Message.Warning("${outcome.document.source}: not canonically formatted" + findings.joinToString("")))
        }
    }

/** For every command that rewrites a file from the DTO, which has no comments; the hint rides in the text. */
fun renderCommentsDropped(source: String): Message =
    Message.Warning(
        "$source: contains YAML comments; they are removed on write\n" +
            formatHint("record notes in schema fields (e.g. comment, analysis)"),
    )

/** [fixHint] names the driver's way to format, such as its task. */
fun renderNotFormatted(
    sources: List<String>,
    fixHint: String,
): Failure = Failure("some Vulnlog files are not formatted: ${sources.joinToString(", ")}", fixHint)

private fun renderFormatFinding(finding: FormatFinding): String {
    val ruleName = finding.rule.name
    val id = ruleName.lowercase().replace('_', '-')
    return if (finding.path.isEmpty()) "[$id] ${finding.message}" else "[$id] ${finding.path}: ${finding.message}"
}
