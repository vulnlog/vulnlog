// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.ChangelogSummary
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ReportingChangelogEntry
import dev.vulnlog.lib.model.reporting.ReportingChangelogProject
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease

private const val UNRELEASED = "unreleased"

/** Renders the changelog as plain text, headed by the project it describes. */
fun formatChangelogText(
    report: ReportingChangelogProject,
    detail: ChangelogDetail,
): String {
    val project = "${report.project.organization} / ${report.project.name}"
    val releases = report.releases.map { release -> textRelease(release, detail) }
    return (listOf(project) + releases).joinToString("\n\n")
}

/** Renders the changelog as Markdown, one section per release. */
fun formatChangelogMarkdown(
    report: ReportingChangelogProject,
    detail: ChangelogDetail,
): String = report.releases.joinToString("\n\n") { release -> markdownRelease(release, detail) }

private fun textRelease(
    release: ReportingChangelogRelease,
    detail: ChangelogDetail,
): String =
    buildString {
        appendLine("${release.fixedIn.value} (${releaseDate(release)})")
        appendLine("  ${summaryLine(release.summary)}")
        release.entries.forEach { entry ->
            appendLine("  ${identifiers(entry)}${textDescription(entry, detail)}")
            if (detail == ChangelogDetail.FULL) {
                entry.note?.let { appendLine("    fix: $it") }
                entry.ref?.let { appendLine("    ref: $it") }
            }
        }
    }.trimEnd('\n')

private fun markdownRelease(
    release: ReportingChangelogRelease,
    detail: ChangelogDetail,
): String =
    buildString {
        appendLine("## [${release.fixedIn.value}] - ${releaseDate(release)}")
        appendLine()
        appendLine("### Security")
        appendLine()
        appendLine("_${summaryLine(release.summary)}_")
        appendLine()
        release.entries.forEach { entry -> appendLine("- ${markdownEntry(entry, detail)}") }
    }.trimEnd('\n')

private fun releaseDate(release: ReportingChangelogRelease): String = release.publishedAt?.toString() ?: UNRELEASED

/** The per-release headline, as in `3 fixed: 1 critical, 2 high`. */
private fun summaryLine(summary: ChangelogSummary): String {
    val bySeverity =
        summary.bySeverity.entries.joinToString(", ") { (severity, count) -> "$count ${canonical(severity)}" }
    return if (bySeverity.isEmpty()) "${summary.total} fixed" else "${summary.total} fixed: $bySeverity"
}

/** The primary identifier followed by the impact and any alias, as in `CVE-2026-001 (high, also GHSA-a)`. */
private fun identifiers(entry: ReportingChangelogEntry): String = "${entry.primaryId.id} ${qualifier(entry)}"

/** One bullet on one line, because a wrapped bullet renders as a single paragraph anyway. */
private fun markdownEntry(
    entry: ReportingChangelogEntry,
    detail: ChangelogDetail,
): String {
    val identifier = "**${entry.primaryId.id}** ${qualifier(entry)}"
    if (detail == ChangelogDetail.BRIEF) return identifier

    val sentences = listOfNotNull(description(entry), entry.note).joinToString(" ", transform = ::sentence)
    val reference = entry.ref?.let { " ($it)" } ?: ""
    return if (sentences.isEmpty()) "$identifier$reference" else "$identifier: $sentences$reference"
}

private fun sentence(text: String): String = if (text.isEmpty() || text.last() in ".!?") text else "$text."

private fun qualifier(entry: ReportingChangelogEntry): String {
    val aliases = entry.aliases.joinToString(", ") { it.id }
    val also = if (aliases.isEmpty()) "" else ", also $aliases"
    return "(${impactLabel(entry.impact)}$also)"
}

private fun textDescription(
    entry: ReportingChangelogEntry,
    detail: ChangelogDetail,
): String = if (detail == ChangelogDetail.BRIEF) "" else description(entry)?.let { " $it" } ?: ""

/** The name and description, whichever the entry records. */
private fun description(entry: ReportingChangelogEntry): String? =
    listOfNotNull(entry.name?.let { "\"$it\"" }, entry.description)
        .joinToString(" ")
        .takeIf { it.isNotEmpty() }

private fun impactLabel(impact: Impact): String =
    when (impact) {
        is Impact.Affected -> canonical(impact.severity)
        is Impact.NotAffected -> "not affected"
        Impact.Unknown -> "not triaged"
    }
