// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.vex.deriveVexStatus
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** The OpenVEX specification this writer emits. */
const val OPEN_VEX_CONTEXT: String = "https://openvex.dev/ns/v0.2.0"

/** The namespace of the document identifiers this writer mints. */
const val OPEN_VEX_ID_PREFIX: String = "https://vulnlog.dev/vex/"

/** Builds the OpenVEX document for [vulnlogFile] under [identity], covering what [scope] selects. */
fun buildOpenVexDocument(
    vulnlogFile: VulnlogFile,
    identity: OpenVexIdentity,
    scope: OpenVexScope = OpenVexScope(),
): OpenVexDocument =
    OpenVexDocument(
        identity = identity,
        author = openVexAuthor(vulnlogFile.project),
        statements = collectOpenVexStatements(vulnlogFile, scope).statements,
    )

/** A document identifier under the Vulnlog namespace. The only impure step of the writer path. */
fun newOpenVexDocumentId(): String = OPEN_VEX_ID_PREFIX + UUID.randomUUID()

/** The identity of this run: the [baseline]'s continued, or a fresh one. [now] is cut to whole seconds. */
fun resolveOpenVexIdentity(
    baseline: OpenVexBaseline?,
    now: Instant,
): OpenVexIdentity {
    val at = now.truncatedTo(ChronoUnit.SECONDS)
    return baseline?.let { nextOpenVexIdentity(it, at) } ?: freshOpenVexIdentity(newOpenVexDocumentId(), at)
}

/** A fresh identity: the given [id], version 1, and no `last_updated` because nothing was updated yet. */
fun freshOpenVexIdentity(
    id: String,
    now: Instant,
): OpenVexIdentity = OpenVexIdentity(id = id, timestamp = now, version = 1)

/** The identity continuing [baseline]: its id and issue time, the next version, and [now] as `last_updated`. */
fun nextOpenVexIdentity(
    baseline: OpenVexBaseline,
    now: Instant,
): OpenVexIdentity =
    OpenVexIdentity(
        id = baseline.id,
        timestamp = baseline.timestamp,
        version = baseline.version + 1,
        lastUpdated = now,
    )

/**
 * Collects one statement per vulnerability entry and release in [scope], anchored to that release's purls, and
 * records what was left out. A release without purls carries no product and anchors nothing. Identical statements
 * collapse, and the result is ordered so the same input always writes the same bytes.
 */
fun collectOpenVexStatements(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope = OpenVexScope(),
): OpenVexCollection {
    val anchors = anchorsOf(vulnlogFile, scope)
    val statements =
        vulnlogFile.vulnerabilities
            .flatMap { vulnEntry -> statementsOf(vulnEntry, anchors) }
            .distinct()
            .sortedWith(compareBy({ it.vulnerability.id }, { it.products.joinToString(",", transform = Purl::value) }))
    return OpenVexCollection(
        scope = scope,
        statements = statements,
        anchors = anchors,
        skippedReleases = skippedReleases(vulnlogFile, scope, anchors.keys),
        skippedEntries = vulnlogFile.vulnerabilities.mapNotNull { vulnEntry -> skippedEntry(vulnEntry, anchors.keys) },
    )
}

/** The author line of the document: the project author, with the contact in parentheses when one is recorded. */
fun openVexAuthor(project: Project): String =
    project.contact?.let { contact -> "${project.author} ($contact)" } ?: project.author

/** The OpenVEX token for a [VexStatus]. */
fun openVexStatus(status: VexStatus): String =
    when (status) {
        VexStatus.UnderInvestigation -> "under_investigation"
        VexStatus.Fixed -> "fixed"
        is VexStatus.NotAffected -> "not_affected"
        is VexStatus.Affected -> "affected"
    }

/** The OpenVEX token for a [VexJustification]. */
fun openVexJustification(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component_not_present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline_mitigations_already_exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable_code_cannot_be_controlled_by_adversary"

        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable_code_not_in_execute_path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable_code_not_present"
    }

/** True when [release] may anchor a statement. An empty release scope covers every release. */
private fun OpenVexScope.covers(release: Release): Boolean = releases.isEmpty() || release in releases

/** The purls each release in scope contributes, keyed by release. A release the tag scope strips bare is dropped. */
private fun anchorsOf(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
): Map<Release, List<Purl>> =
    vulnlogFile.releases
        .filter { entry -> scope.covers(entry.id) }
        .associateBy(ReleaseEntry::id) { entry -> scopedPurls(entry, scope.tags) }
        .filterValues { purls -> purls.isNotEmpty() }

/** The purls of [entry] the tag scope keeps. Without tags every purl is kept. */
private fun scopedPurls(
    entry: ReleaseEntry,
    tags: Set<Tag>,
): List<Purl> =
    entry.purls
        .filter { purlEntry -> tags.isEmpty() || purlEntry.tags.any { tag -> tag in tags } }
        .map(PurlEntry::purl)
        .sortedBy(Purl::value)

private fun statementsOf(
    vulnEntry: VulnerabilityEntry,
    anchors: Map<Release, List<Purl>>,
): List<OpenVexStatement> =
    coveredReleases(vulnEntry).mapNotNull { release ->
        anchors[release]?.let { products ->
            OpenVexStatement(
                vulnerability = vulnEntry.id,
                products = products,
                status = deriveVexStatus(vulnEntry, release),
            )
        }
    }

/** The releases in scope an entry speaks about that anchor nothing, in the order the file declares them. */
private fun skippedReleases(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
    anchoring: Set<Release>,
): List<Release> {
    val covered = vulnlogFile.vulnerabilities.flatMap(::coveredReleases).toSet()
    return vulnlogFile.releases
        .map(ReleaseEntry::id)
        .filter { release -> release in covered && scope.covers(release) && release !in anchoring }
}

private fun skippedEntry(
    vulnEntry: VulnerabilityEntry,
    anchoring: Set<Release>,
): OpenVexSkippedEntry? {
    val covered = coveredReleases(vulnEntry)
    return when {
        covered.isEmpty() -> OpenVexSkippedEntry.NoRelease(vulnEntry.id)
        covered.none { release -> release in anchoring } -> OpenVexSkippedEntry.NoAnchoredRelease(vulnEntry.id)
        else -> null
    }
}

/** The releases an entry makes a statement about: the ones it affects, plus the one that fixed it. */
private fun coveredReleases(vulnEntry: VulnerabilityEntry): List<Release> =
    (vulnEntry.releases + listOfNotNull(vulnEntry.resolution?.release)).distinct()
