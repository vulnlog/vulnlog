// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.model.finding.FindingSeverity

data class Failure(
    val message: String,
    val hint: String,
)

fun formatFailureLines(failures: List<Failure>): List<String> =
    failures.flatMap { failure ->
        listOf(formatMessage(FindingSeverity.ERROR, failure.message), formatHint(failure.hint))
    }

fun formatFailureMessage(failures: List<Failure>): String =
    failures.joinToString(" ") { failure -> "${sentence(failure.message)} ${sentence(failure.hint)}" }

private fun sentence(text: String): String {
    val capitalized = text.replaceFirstChar(Char::uppercase)
    return if (capitalized.endsWith('.')) capitalized else "$capitalized."
}
