// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.vex.openvex.openVexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry

/**
 * Renders what a run reports before it writes or fails, in the order drivers print it: the scope, the releases the
 * document leaves out, the anchoring releases, the skipped entries, and the statement counts of a built document. A
 * run rejected before it collected anything reports nothing. Shared by the CLI and the Gradle plugin.
 */
fun renderOpenVexReport(outcome: OpenVexOutcome): List<OpenVexLine> =
    when (outcome) {
        is FilterRejected, is OpenVexOutcome.BaselineRejected -> emptyList()
        is OpenVexOutcome.NoStatementApplies -> collectionLines(outcome.collection)
        is OpenVexOutcome.Revised -> collectionLines(outcome.collection) + countLine(outcome.document)
        is OpenVexOutcome.Unchanged -> collectionLines(outcome.collection) + countLine(outcome.document)
    }

/**
 * Renders one diagnostic line per active scope dimension, stating what it resolved to.
 * An inactive dimension produces no line, so an unscoped run stays silent. Mirrors renderFilterResolution.
 */
fun renderOpenVexScope(scope: OpenVexScope): List<String> =
    listOfNotNull(
        scope.releases
            .takeIf { it.isNotEmpty() }
            ?.let { releases -> "release scope: ${releases.joinToString(", ") { it.value }}" },
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

/**
 * Renders the error naming why the baseline at [target] cannot be continued. Shared by the CLI and the Gradle plugin,
 * so both reject a baseline in the same words.
 */
fun renderOpenVexBaselineProblem(
    target: String,
    problem: OpenVexBaselineProblem,
): String =
    when (problem) {
        OpenVexBaselineProblem.NotOpenVex -> "baseline '$target' is not an OpenVEX document"

        is OpenVexBaselineProblem.OtherFormatVersion ->
            "baseline '$target' is an OpenVEX ${problem.declared} document, " +
                "but this run writes OpenVEX ${problem.required.version}"

        is OpenVexBaselineProblem.InvalidIdentity -> renderInvalidIdentity(target, problem)
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

/** Renders the hint that follows "no statement applies", naming what to change. */
fun renderOpenVexEmptyHint(reason: OpenVexEmptyReason): String =
    when (reason) {
        OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS -> "declare 'purls' on the releases you want the document to cover"
        OpenVexEmptyReason.NO_PURL_CARRIES_TAG -> "no release purl in scope carries one of the requested tags"
        OpenVexEmptyReason.NO_ENTRY_IN_RELEASE_SCOPE -> "no vulnerability entry applies to the release in scope"
        OpenVexEmptyReason.NO_ENTRY_ON_ANCHORED_RELEASE ->
            "no vulnerability entry references a release that declares purls"
    }

/** Renders one diagnostic line for a written document, the counterpart of the suppression writer's. */
fun renderOpenVexWritten(
    target: String,
    document: OpenVexDocument,
): String =
    "wrote $target: openvex format, version ${document.identity.version.value}, " +
        pluralize(document.statements.size, "statement")

private fun collectionLines(collection: OpenVexCollection): List<OpenVexLine> =
    renderOpenVexScope(collection.scope).map(OpenVexLine::Verbose) +
        listOfNotNull(
            renderOpenVexSkippedReleases(collection)?.let(OpenVexLine::Warning),
            renderOpenVexProducts(collection)?.let(OpenVexLine::Verbose),
        ) +
        renderOpenVexSkippedEntries(collection).map(OpenVexLine::Debug)

private fun countLine(document: OpenVexDocument): OpenVexLine =
    OpenVexLine.Verbose(renderOpenVexStatementCounts(document))

private fun renderInvalidIdentity(
    target: String,
    problem: OpenVexBaselineProblem.InvalidIdentity,
): String {
    val (field, expected) =
        when (problem.field) {
            OpenVexIdentityField.ID -> "@id" to "an absolute IRI"
            OpenVexIdentityField.TIMESTAMP -> "timestamp" to "an RFC 3339 timestamp"
            OpenVexIdentityField.VERSION -> "version" to "a whole number from 1 to ${Int.MAX_VALUE - 1}"
        }
    val found = problem.value?.let { value -> "an invalid '$field' '$value'" } ?: "no '$field'"
    return "baseline '$target' has $found, expected $expected"
}

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
