// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.vex.filterReleasePurlsMatchingVulnerabilityEntryTags
import dev.vulnlog.lib.core.vex.releaseStatuses
import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.ReleaseStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexReleaseScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import java.time.LocalDate

fun collectOpenVexStatements(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope = OpenVexScope(),
): OpenVexCollection {
    val anchors = anchorsOf(vulnlogFile, scope)
    val entries = vulnlogFile.vulnerabilities.filter { vulnEntry -> scope.coversAnyOf(vulnEntry.tags) }
    val statuses = entries.associateWith { vulnEntry -> releaseStatuses(vulnEntry, vulnlogFile) }
    val matchingPurls = entries.associateWith { vulnEntry -> matchingReleasePurlsOf(vulnEntry, vulnlogFile, scope) }
    val statements =
        statuses
            .flatMap { (vulnEntry, releaseStatuses) ->
                statementsOf(vulnEntry, releaseStatuses, matchingPurls.getValue(vulnEntry))
            }.distinct()
            .sortedWith(
                compareBy(
                    { it.vulnerability.id.id },
                    { it.products.joinToString(",", transform = Purl::value) },
                    { it.timestamp.statedDate() },
                    { vexStatusKind(it.status) },
                ),
            )
    return OpenVexCollection(
        scope = scope,
        statements = statements,
        anchors = anchors,
        unpublishedReleases = unpublishedReleases(vulnlogFile, scope),
        skippedReleases = skippedReleases(vulnlogFile, scope, statuses.values.flatten(), anchors.keys),
        skippedEntries =
            statuses.mapNotNull { (vulnEntry, releaseStatuses) ->
                skippedEntry(vulnEntry, releaseStatuses, anchors.keys, matchingPurls.getValue(vulnEntry).keys)
            },
    )
}

fun openVexEmptyReason(
    vulnlogFile: VulnlogFile,
    collection: OpenVexCollection,
): OpenVexEmptyReason =
    when {
        vulnlogFile.releases.none { it.purls.isNotEmpty() } -> OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS
        collection.scope.release == OpenVexReleaseScope.Published &&
            vulnlogFile.releases.none { it.publicationDate != null && it.purls.isNotEmpty() } ->
            OpenVexEmptyReason.NO_PUBLISHED_RELEASE_DECLARES_PURLS

        collection.skippedEntries.any {
            it is OpenVexSkippedEntry.NoTags || it is OpenVexSkippedEntry.NoMatchingReleasePurl
        } -> OpenVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS

        collection.scope.tags.isNotEmpty() -> OpenVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE
        collection.scope.release is OpenVexReleaseScope.Named -> OpenVexEmptyReason.NO_ENTRY_IN_RELEASE_SCOPE
        else -> OpenVexEmptyReason.NO_ENTRY_ON_ANCHORED_RELEASE
    }

/** The specification forbids re-dating a statement a revision does not touch, so an equal one keeps its time. */
fun carryOverOpenVexTimestamps(
    statements: List<OpenVexStatement>,
    baseline: OpenVexBaseline?,
): List<OpenVexStatement> {
    if (baseline == null) return statements
    // Keyed as this revision issues a statement, so only an undated one finds a carried time.
    val carried =
        baseline.statements
            .mapNotNull { statement ->
                (statement.timestamp as? OpenVexStatementTime.Carried)?.let { time ->
                    statement.copy(timestamp = OpenVexStatementTime.Issued) to time
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, times) -> times.minBy(OpenVexStatementTime.Carried::at) }
    return statements.map { statement ->
        carried[statement]?.let { time -> statement.copy(timestamp = time) }
            ?: statement
    }
}

private fun OpenVexStatementTime.statedDate(): LocalDate? =
    when (this) {
        is OpenVexStatementTime.Stated -> date
        is OpenVexStatementTime.Carried, OpenVexStatementTime.Issued -> null
    }

private fun OpenVexScope.covers(release: ReleaseEntry): Boolean =
    when (val scoped = this.release) {
        OpenVexReleaseScope.Published -> release.publicationDate != null
        is OpenVexReleaseScope.Named -> release.id == scoped.release
    }

private fun OpenVexScope.coversAnyOf(tags: List<Tag>): Boolean = this.tags.isEmpty() || tags.any { it in this.tags }

private fun anchorsOf(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
): Map<Release, List<Purl>> =
    vulnlogFile.releases
        .filter { release -> scope.covers(release) }
        .associateBy(ReleaseEntry::id) { release -> scopedPurls(release.purls, scope) }
        .filterValues { purls -> purls.isNotEmpty() }

private fun matchingReleasePurlsOf(
    vulnEntry: VulnerabilityEntry,
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
): Map<Release, List<Purl>> =
    vulnlogFile.releases
        .filter { release -> scope.covers(release) }
        .associateBy(ReleaseEntry::id) { release ->
            scopedPurls(filterReleasePurlsMatchingVulnerabilityEntryTags(release, vulnEntry), scope)
        }.filterValues { purls -> purls.isNotEmpty() }

private fun scopedPurls(
    purls: List<PurlEntry>,
    scope: OpenVexScope,
): List<Purl> =
    purls
        .filter { purlEntry -> scope.coversAnyOf(purlEntry.tags) }
        .map(PurlEntry::purl)
        .sortedBy(Purl::value)

private fun statementsOf(
    vulnEntry: VulnerabilityEntry,
    releaseStatuses: List<ReleaseStatus>,
    matchingPurls: Map<Release, List<Purl>>,
): List<OpenVexStatement> =
    releaseStatuses.mapNotNull { releaseStatus ->
        matchingPurls[releaseStatus.release]?.let { products ->
            OpenVexStatement(
                vulnerability =
                    OpenVexVulnerability(
                        id = vulnEntry.id,
                        aliases = vulnEntry.aliases.sortedBy(VulnId::id),
                        description = vulnEntry.description,
                    ),
                timestamp = releaseStatus.since?.let(OpenVexStatementTime::Stated) ?: OpenVexStatementTime.Issued,
                products = products,
                subcomponents = vulnEntry.packages.sortedBy(Purl::value),
                status = releaseStatus.status,
            )
        }
    }

private fun unpublishedReleases(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
): List<Release> =
    when (scope.release) {
        OpenVexReleaseScope.Published ->
            vulnlogFile.releases.filter { release -> release.publicationDate == null }.map(ReleaseEntry::id)

        is OpenVexReleaseScope.Named -> emptyList()
    }

private fun skippedReleases(
    vulnlogFile: VulnlogFile,
    scope: OpenVexScope,
    releaseStatuses: List<ReleaseStatus>,
    anchoring: Set<Release>,
): List<Release> {
    val covered = releaseStatuses.map(ReleaseStatus::release).toSet()
    return vulnlogFile.releases
        .filter { release -> release.id in covered && scope.covers(release) && release.id !in anchoring }
        .map(ReleaseEntry::id)
}

private fun skippedEntry(
    vulnEntry: VulnerabilityEntry,
    releaseStatuses: List<ReleaseStatus>,
    anchoring: Set<Release>,
    withMatchingPurls: Set<Release>,
): OpenVexSkippedEntry? =
    when {
        releaseStatuses.isEmpty() -> OpenVexSkippedEntry.NoRelease(vulnEntry.id)
        releaseStatuses.none { it.release in anchoring } -> OpenVexSkippedEntry.NoAnchoredRelease(vulnEntry.id)
        releaseStatuses.any { it.release in withMatchingPurls } -> null
        vulnEntry.tags.isEmpty() -> OpenVexSkippedEntry.NoTags(vulnEntry.id)
        else -> OpenVexSkippedEntry.NoMatchingReleasePurl(vulnEntry.id)
    }
