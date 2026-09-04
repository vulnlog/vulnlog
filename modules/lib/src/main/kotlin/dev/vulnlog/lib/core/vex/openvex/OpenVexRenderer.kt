// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry

/**
 * Renders one diagnostic line per active scope dimension, stating what it resolved to.
 * An inactive dimension produces no line, so an unscoped run stays silent. Mirrors renderFilterResolution.
 */
fun renderOpenVexScope(scope: OpenVexScope): List<String> =
    listOfNotNull(
        scope.releases
            .takeIf { it.isNotEmpty() }
            ?.let { releases -> "as-of scope expanded to releases: ${releases.joinToString(", ") { it.value }}" },
        scope.tags
            .takeIf { it.isNotEmpty() }
            ?.let { tags -> "tag scope matched tags: ${tags.joinToString(", ") { it.value }}" },
    )

/**
 * Renders one diagnostic line naming the releases that anchor the document and how many purls each contributes,
 * or null when none does. Shared by the CLI and the Gradle plugin.
 */
fun renderOpenVexProducts(collection: OpenVexCollection): String? {
    if (collection.anchors.isEmpty()) return null
    val detail =
        collection.anchors.entries.joinToString(", ") { "'${it.key.value}' (${pluralize(it.value.size, "purl")})" }
    return "anchored on ${pluralize(collection.anchors.size, "release")} with purls: $detail"
}

/**
 * Renders the warning naming the releases in scope that carry no purl, or null when every one does.
 * They drop out of the document silently, so this is the line that names them.
 */
fun renderOpenVexSkippedReleases(collection: OpenVexCollection): String? {
    if (collection.skippedReleases.isEmpty()) return null
    val names = collection.skippedReleases.joinToString(", ") { "'${it.value}'" }
    val subject = if (collection.scope.tags.isEmpty()) "releases without purls" else "releases without purls in scope"
    return "$subject are not part of the document: $names"
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
fun renderOpenVexSkippedEntries(collection: OpenVexCollection): List<String> =
    collection.skippedEntries.map(::renderSkippedEntry).sorted()

/** Renders the hint that follows "no statement applies", naming the most likely cause. */
fun renderOpenVexEmptyHint(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
): String =
    when {
        vulnlogFile.releases.none { it.purls.isNotEmpty() } ->
            "declare 'purls' on the releases you want the document to cover"

        scope.tags.isNotEmpty() -> "no release purl in scope carries one of the requested tags"
        scope.releases.isNotEmpty() -> "no vulnerability entry references a release in scope that declares purls"
        else -> "no vulnerability entry references a release that declares purls"
    }

/** Renders one diagnostic line for a written document, the counterpart of the suppression writer's. */
fun renderOpenVexWritten(
    target: String,
    document: OpenVexDocument,
): String =
    "wrote $target: openvex format, version ${document.identity.version}, " +
        pluralize(document.statements.size, "statement")

private fun renderSkippedEntry(entry: OpenVexSkippedEntry): String =
    when (entry) {
        is OpenVexSkippedEntry.NoRelease -> "skipped ${entry.id.canonical()}: it references no release"
        is OpenVexSkippedEntry.NoAnchoredRelease ->
            "skipped ${entry.id.canonical()}: no release it applies to declares purls in scope"
    }

private fun pluralize(
    count: Int,
    noun: String,
): String = if (count == 1) "1 $noun" else "$count ${noun}s"
