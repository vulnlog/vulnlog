// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.CycloneDxVexOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentityField
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexSkippedEntry

fun renderCycloneDxVexMessages(outcome: CycloneDxVexOutcome): List<Message> =
    when (outcome) {
        is FilterRejected, is CycloneDxVexOutcome.BaselineRejected -> emptyList()
        is CycloneDxVexOutcome.NoVulnerabilityApplies -> collectionMessages(outcome.collection)
        is CycloneDxVexOutcome.Generated -> collectionMessages(outcome.collection) + countMessage(outcome.collection)
    }

/** [baseline] and [baselineOption] name the file and the setting it came from; only a rejected baseline uses them. */
fun renderCycloneDxVexFailure(
    failed: CycloneDxVexOutcome.Failed,
    baseline: String,
    baselineOption: String,
    tagOption: String,
): List<Failure> =
    when (failed) {
        is FilterRejected -> renderFilterProblems(failed.problems)

        is CycloneDxVexOutcome.BaselineRejected ->
            listOf(Failure(baselineProblem(baseline, failed.problem), omitBaselineHint(baselineOption)))

        is CycloneDxVexOutcome.NoVulnerabilityApplies ->
            listOf(Failure("no vulnerability applies", emptyHint(failed.reason, failed.collection, tagOption)))
    }

fun renderCycloneDxVexWritten(
    target: String,
    outcome: CycloneDxVexOutcome.Generated,
): Message =
    Message.Verbose(
        "wrote $target: cyclonedx format, version ${outcome.version.value}, " +
            pluralize(outcome.collection.vulnerabilities.size, "vulnerability", "vulnerabilities"),
    )

private fun baselineProblem(
    target: String,
    problem: CycloneDxVexBaselineProblem,
): String =
    when (problem) {
        CycloneDxVexBaselineProblem.NotCycloneDx -> "baseline '$target' is not a CycloneDX document"

        is CycloneDxVexBaselineProblem.OtherFormatVersion ->
            "baseline '$target' is a CycloneDX ${problem.declared} document, " +
                "but this run writes CycloneDX ${problem.required.version}"

        is CycloneDxVexBaselineProblem.InvalidIdentity -> {
            val (field, expected) =
                when (problem.field) {
                    CycloneDxVexIdentityField.SERIAL_NUMBER -> "serialNumber" to "a urn:uuid"
                    CycloneDxVexIdentityField.TIMESTAMP -> "metadata.timestamp" to "an RFC 3339 timestamp"
                    CycloneDxVexIdentityField.VERSION -> "version" to "a whole number from 1 to ${Int.MAX_VALUE - 1}"
                }
            val found = problem.value?.let { value -> "an invalid '$field' '$value'" } ?: "no '$field'"
            "baseline '$target' has $found, expected $expected"
        }
    }

private fun emptyHint(
    reason: CycloneDxVexEmptyReason,
    collection: CycloneDxVexCollection,
    tagOption: String,
): String {
    val release = collection.scope.release.value
    return when (reason) {
        CycloneDxVexEmptyReason.RELEASE_DECLARES_NO_PURLS -> "declare 'purls' on release '$release'"
        CycloneDxVexEmptyReason.NO_RELEASE_PURL_IN_TAG_SCOPE ->
            "no purl of release '$release' carries one of the requested tags, pass another $tagOption or omit it"

        CycloneDxVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS ->
            "tag the vulnerability entries with the tags of the release purls they apply to"

        CycloneDxVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE ->
            "no vulnerability entry and release purl in scope share one of the requested tags"

        CycloneDxVexEmptyReason.NO_ENTRY_ON_RELEASE -> "no vulnerability entry applies to release '$release'"
    }
}

private fun collectionMessages(collection: CycloneDxVexCollection): List<Message> =
    scopeLines(collection).map(Message::Verbose) +
        listOfNotNull(productLine(collection)?.let(Message::Verbose)) +
        collection.skippedEntries
            .map(::skippedEntryLine)
            .sorted()
            .map(Message::Debug)

private fun scopeLines(collection: CycloneDxVexCollection): List<String> =
    listOfNotNull(
        "release scope: ${collection.scope.release.value}",
        collection.scope.tags
            .takeIf { it.isNotEmpty() }
            ?.let { tags -> "tag scope matched tags: ${tags.joinToString(", ") { it.value }}" },
    )

/** No warning where the release has no purls: the run fails and says so. */
private fun productLine(collection: CycloneDxVexCollection): String? {
    if (collection.productPurls.isEmpty()) return null
    val purls = collection.productPurls.joinToString(", ") { it.value }
    return "product: release '${collection.scope.release.value}' with ${pluralize(
        collection.productPurls.size,
        "purl",
    )}: $purls"
}

private fun skippedEntryLine(entry: CycloneDxVexSkippedEntry): String =
    when (entry) {
        is CycloneDxVexSkippedEntry.NoRelease -> "skipped ${entry.id.canonical()}: it references no release"
        is CycloneDxVexSkippedEntry.NotOnRelease ->
            "skipped ${entry.id.canonical()}: it does not apply to the release in scope"

        is CycloneDxVexSkippedEntry.NoTags -> "skipped ${entry.id.canonical()}: it has no tags to match a release purl"
        is CycloneDxVexSkippedEntry.NoMatchingReleasePurl ->
            "skipped ${entry.id.canonical()}: no release purl in scope shares one of its tags"
    }

private fun countMessage(collection: CycloneDxVexCollection): Message {
    val byState =
        collection.vulnerabilities
            .groupingBy { vexStatusKind(it.status).name.lowercase() }
            .eachCount()
    val detail = byState.entries.sortedBy { it.key }.joinToString(", ") { "${it.value} ${it.key}" }
    val count = pluralize(collection.vulnerabilities.size, "vulnerability", "vulnerabilities")
    return Message.Verbose("collected $count: $detail")
}
