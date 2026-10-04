// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.finding.FindingSeverity

/** One vocabulary for both drivers, so the CLI and the Gradle plugin word the same event identically. */
enum class StatusVerb(
    val display: String,
) {
    CREATED("Created"),
    WROTE("Wrote"),
    FORMATTED("Formatted"),
    UNCHANGED("Unchanged"),
    COPIED("Copied"),
    ADDED("Added"),
    UPDATED("Updated"),
    VALIDATED("Validated"),
}

/** `Created: vulnlog.yaml` */
fun formatStatus(
    verb: StatusVerb,
    subject: String,
): String = "${verb.display}: $subject"

/** `error: vulnlog.yaml: vulnerabilities[3].resolution.in: release '9.9.9' is not defined` */
fun formatFinding(
    severity: FindingSeverity,
    file: String,
    location: String? = null,
    message: String,
): String {
    val locationPart = location?.takeIf(String::isNotBlank)?.let { "$it: " } ?: ""
    return "${severityLabel(severity)}: $file: $locationPart$message"
}

/** `error: <message>` */
fun formatMessage(
    severity: FindingSeverity,
    message: String,
): String = "${severityLabel(severity)}: $message"

/** `  hint: run vulnlog fmt`, indented under the line it follows. */
fun formatHint(nextStep: String): String = "  hint: $nextStep"

/** `2 errors, 1 warning`; a count of zero is left out. */
fun formatSummary(
    errors: Int,
    warnings: Int,
    infos: Int = 0,
): String =
    listOf(errors to "error", warnings to "warning", infos to "info")
        .filter { (count, _) -> count > 0 }
        .joinToString(", ") { (count, noun) -> pluralize(count, noun) }

/** `1 entry`, `0 entries`, `3 entries` */
fun pluralize(
    count: Int,
    singular: String,
    plural: String = singular + "s",
): String = if (count == 1) "1 $singular" else "$count $plural"

private fun severityLabel(severity: FindingSeverity): String =
    when (severity) {
        FindingSeverity.ERROR -> "error"
        FindingSeverity.WARNING -> "warning"
        FindingSeverity.INFO -> "info"
    }
