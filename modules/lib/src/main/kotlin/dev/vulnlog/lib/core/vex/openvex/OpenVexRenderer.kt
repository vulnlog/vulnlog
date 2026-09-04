// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument

/**
 * Renders one diagnostic line naming the releases that anchor the document and how many purls each contributes,
 * or null when no release declares one. Shared by the CLI and the Gradle plugin.
 */
fun renderOpenVexProducts(vulnlogFile: VulnlogFile): String? {
    val anchors = vulnlogFile.releases.filter { it.purls.isNotEmpty() }
    if (anchors.isEmpty()) return null
    val detail = anchors.joinToString(", ") { "'${it.id.value}' (${pluralize(it.purls.size, "purl")})" }
    return "anchored on ${pluralize(anchors.size, "release")} with purls: $detail"
}

/** Renders one diagnostic line stating how many statements the document holds, broken down by status. */
fun renderOpenVexStatementCounts(document: OpenVexDocument): String {
    val byStatus =
        document.statements
            .groupingBy { openVexStatus(it.status) }
            .eachCount()
    val detail = byStatus.entries.sortedBy { it.key }.joinToString(", ") { "${it.value} ${it.key}" }
    return "collected ${pluralize(document.statements.size, "statement")}: $detail"
}

/**
 * Renders one diagnostic line per vulnerability entry that contributed no statement, stating why.
 * These are the entries missing from the document, so this is the line to read when one is expected and absent.
 */
fun renderOpenVexSkippedEntries(vulnlogFile: VulnlogFile): List<String> =
    vulnlogFile.vulnerabilities.mapNotNull { vulnEntry -> skipReason(vulnlogFile, vulnEntry) }.sorted()

/** Renders one diagnostic line for a written document, the counterpart of the suppression writer's. */
fun renderOpenVexWritten(
    target: String,
    document: OpenVexDocument,
): String = "wrote $target: openvex format, ${pluralize(document.statements.size, "statement")}"

private fun skipReason(
    vulnlogFile: VulnlogFile,
    vulnEntry: VulnerabilityEntry,
): String? {
    val covered = coveredReleases(vulnEntry)
    val withoutPurls = releasesWithoutPurls(vulnlogFile, vulnEntry)
    return when {
        covered.isEmpty() -> "skipped ${vulnEntry.id.id}: it references no release"
        covered.size == withoutPurls.size ->
            "skipped ${vulnEntry.id.id}: no release it applies to declares purls"

        else -> null
    }
}

private fun pluralize(
    count: Int,
    noun: String,
): String = if (count == 1) "1 $noun" else "$count ${noun}s"
