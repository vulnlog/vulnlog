// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry

fun renderOpenVexMessages(outcome: OpenVexOutcome): List<Message> =
    when (outcome) {
        is FilterRejected, is OpenVexOutcome.BaselineRejected -> emptyList()
        is OpenVexOutcome.NoStatementApplies -> collectionMessages(outcome.collection)
        is OpenVexOutcome.Generated -> collectionMessages(outcome.collection) + countMessage(outcome.collection)
    }

/** [baseline] and [baselineOption] name the file and the setting it came from; only a rejected baseline uses them. */
fun renderOpenVexFailure(
    failed: OpenVexOutcome.Failed,
    baseline: String,
    baselineOption: String,
): List<Failure> =
    when (failed) {
        is FilterRejected -> renderFilterProblems(failed.problems)

        is OpenVexOutcome.BaselineRejected ->
            listOf(Failure(baselineProblem(baseline, failed.problem), omitHint(baselineOption)))

        is OpenVexOutcome.NoStatementApplies -> listOf(Failure("no statement applies", emptyHint(failed.reason)))
    }

fun renderOpenVexBaselineFailure(
    unavailable: OpenVexBaselineRead.Unavailable,
    baseline: String,
    baselineOption: String,
): Failure =
    when (unavailable) {
        OpenVexBaselineRead.Absent -> Failure("baseline '$baseline' does not exist", omitHint(baselineOption))

        is OpenVexBaselineRead.Unreadable ->
            Failure(
                "cannot read baseline '$baseline': ${unavailable.reason}",
                "pass a readable file to $baselineOption, or omit it to issue a new document",
            )
    }

fun renderOpenVexNewDocument(baseline: String): Message =
    Message.Verbose("baseline '$baseline' does not exist yet, issuing a new document")

fun renderOpenVexWritten(
    target: String,
    outcome: OpenVexOutcome.Generated,
): Message =
    Message.Verbose(
        "wrote $target: openvex format, version ${outcome.version.value}, " +
            pluralize(outcome.collection.statements.size, "statement"),
    )

private fun omitHint(baselineOption: String): String = "omit $baselineOption to issue a new document"

private fun baselineProblem(
    target: String,
    problem: OpenVexBaselineProblem,
): String =
    when (problem) {
        OpenVexBaselineProblem.NotOpenVex -> "baseline '$target' is not an OpenVEX document"

        is OpenVexBaselineProblem.OtherFormatVersion ->
            "baseline '$target' is an OpenVEX ${problem.declared} document, " +
                "but this run writes OpenVEX ${problem.required.version}"

        is OpenVexBaselineProblem.InvalidIdentity -> {
            val (field, expected) =
                when (problem.field) {
                    OpenVexIdentityField.ID -> "@id" to "an absolute IRI"
                    OpenVexIdentityField.TIMESTAMP -> "timestamp" to "an RFC 3339 timestamp"
                    OpenVexIdentityField.VERSION -> "version" to "a whole number from 1 to ${Int.MAX_VALUE - 1}"
                }
            val found = problem.value?.let { value -> "an invalid '$field' '$value'" } ?: "no '$field'"
            "baseline '$target' has $found, expected $expected"
        }
    }

private fun emptyHint(reason: OpenVexEmptyReason): String =
    when (reason) {
        OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS -> "declare 'purls' on the releases you want the document to cover"
        OpenVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS ->
            "tag the vulnerability entries with the tags of the release purls they apply to"

        OpenVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE ->
            "no vulnerability entry and release purl in scope share one of the requested tags"

        OpenVexEmptyReason.NO_ENTRY_IN_RELEASE_SCOPE -> "no vulnerability entry applies to the release in scope"
        OpenVexEmptyReason.NO_ENTRY_ON_ANCHORED_RELEASE ->
            "no vulnerability entry references a release that declares purls"
    }

private fun collectionMessages(collection: OpenVexCollection): List<Message> =
    scopeLines(collection).map(Message::Verbose) +
        listOfNotNull(
            skippedReleasesLine(collection)?.let(Message::Warning),
            anchorsLine(collection)?.let(Message::Verbose),
        ) +
        collection.skippedEntries
            .map(::skippedEntryLine)
            .sorted()
            .map(Message::Debug)

private fun scopeLines(collection: OpenVexCollection): List<String> =
    listOfNotNull(
        collection.scope.releases
            .takeIf { it.isNotEmpty() }
            ?.let { releases -> "release scope: ${releases.joinToString(", ") { it.value }}" },
        collection.scope.tags
            .takeIf { it.isNotEmpty() }
            ?.let { tags -> "tag scope matched tags: ${tags.joinToString(", ") { it.value }}" },
    )

/** A warning, because a release without purls drops out of the document without any other trace. */
private fun skippedReleasesLine(collection: OpenVexCollection): String? {
    if (collection.skippedReleases.isEmpty()) return null
    val names = collection.skippedReleases.joinToString(", ") { "'${it.value}'" }
    val subject = if (collection.scope.tags.isEmpty()) "releases without purls" else "releases without purls in scope"
    return "$subject are not part of the document: $names"
}

private fun anchorsLine(collection: OpenVexCollection): String? {
    if (collection.anchors.isEmpty()) return null
    val detail =
        collection.anchors.entries.joinToString(", ") { "'${it.key.value}' (${pluralize(it.value.size, "purl")})" }
    return "anchored on ${pluralize(collection.anchors.size, "release")} with purls: $detail"
}

private fun skippedEntryLine(entry: OpenVexSkippedEntry): String =
    when (entry) {
        is OpenVexSkippedEntry.NoRelease -> "skipped ${entry.id.canonical()}: it references no release"
        is OpenVexSkippedEntry.NoAnchoredRelease ->
            "skipped ${entry.id.canonical()}: no release it applies to declares purls in scope"

        is OpenVexSkippedEntry.NoTags -> "skipped ${entry.id.canonical()}: it has no tags to match a release purl"
        is OpenVexSkippedEntry.NoMatchingReleasePurl ->
            "skipped ${entry.id.canonical()}: no release purl in scope shares one of its tags"
    }

private fun countMessage(collection: OpenVexCollection): Message {
    val byStatus =
        collection.statements
            .groupingBy { vexStatusKind(it.status).name.lowercase() }
            .eachCount()
    val detail = byStatus.entries.sortedBy { it.key }.joinToString(", ") { "${it.value} ${it.key}" }
    return Message.Verbose("collected ${pluralize(collection.statements.size, "statement")}: $detail")
}

private fun pluralize(
    count: Int,
    noun: String,
): String = if (count == 1) "1 $noun" else "$count ${noun}s"
