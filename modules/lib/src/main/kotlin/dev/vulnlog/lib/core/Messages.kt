// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.result.Severity

/**
 * The verbs allowed on status lines. One vocabulary for every surface, so the CLI and the
 * Gradle plugin word the same event identically.
 */
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

/** Status line for a successful action: `Created: vulnlog.yaml`. */
fun formatStatus(
    verb: StatusVerb,
    subject: String,
): String = "${verb.display}: $subject"

/** Finding line: `error: vulnlog.yaml: vulnerabilities[3].resolution.in: release '9.9.9' is not defined`. */
fun formatFinding(
    severity: Severity,
    file: String,
    location: String? = null,
    message: String,
): String {
    val locationPart = location?.takeIf(String::isNotBlank)?.let { "$it: " } ?: ""
    return "${severityLabel(severity)}: $file: $locationPart$message"
}

/** Severity-prefixed message without a file anchor: `error: <message>`. */
fun formatMessage(
    severity: Severity,
    message: String,
): String = "${severityLabel(severity)}: $message"

/** Hint line following a finding, stating the next step: `  hint: run vulnlog fmt`. */
fun formatHint(nextStep: String): String = "  hint: $nextStep"

/** Summary line after findings with real plurals: `2 errors, 1 warning`. Zero counts are omitted. */
fun formatSummary(
    errors: Int,
    warnings: Int,
    infos: Int = 0,
): String =
    listOf(
        pluralize(errors, "error"),
        pluralize(warnings, "warning"),
        pluralize(infos, "info"),
    ).filterNotNull().joinToString(", ")

/** Counted noun with a real plural: `1 entry`, `3 entries`. Returns null for a zero count. */
fun pluralize(
    count: Int,
    singular: String,
    plural: String = singular + "s",
): String? =
    when {
        count == 0 -> null
        count == 1 -> "$count $singular"
        else -> "$count $plural"
    }

fun severityLabel(severity: Severity): String =
    when (severity) {
        Severity.ERROR -> "error"
        Severity.WARNING -> "warning"
        Severity.INFO -> "info"
    }
