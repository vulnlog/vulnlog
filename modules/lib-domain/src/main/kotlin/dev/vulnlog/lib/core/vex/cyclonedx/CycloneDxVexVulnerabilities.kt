// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.cyclonedx

import dev.vulnlog.lib.core.vex.filterReleasePurlsMatchingVulnerabilityEntryTags
import dev.vulnlog.lib.core.vex.releaseStatuses
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.ReleaseStatus
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexSkippedEntry
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability

/**
 * One vulnerability per entry that applies to the release in scope and shares a tag with one of its purls in scope,
 * as an OpenVEX statement would. The release purls do not appear on the vulnerability: the document names them once
 * as the product.
 */
fun collectCycloneDxVexVulnerabilities(
    vulnlogFile: VulnlogFile,
    scope: CycloneDxVexScope,
): CycloneDxVexCollection {
    val release = vulnlogFile.releases.firstOrNull { release -> release.id == scope.release }
    val productPurls = release?.let { scopedPurls(it.purls, scope) }.orEmpty()
    val entries = vulnlogFile.vulnerabilities.filter { vulnEntry -> scope.coversAnyOf(vulnEntry.tags) }
    val statuses = entries.associateWith { vulnEntry -> releaseStatuses(vulnEntry, vulnlogFile) }
    val matching =
        entries.associateWith { vulnEntry ->
            release
                ?.let {
                    scopedPurls(
                        filterReleasePurlsMatchingVulnerabilityEntryTags(it, vulnEntry),
                        scope,
                    )
                }.orEmpty()
        }
    val vulnerabilities =
        statuses
            .mapNotNull { (vulnEntry, releaseStatuses) ->
                statusOn(releaseStatuses, scope)
                    ?.takeIf { matching.getValue(vulnEntry).isNotEmpty() }
                    ?.let { status -> vulnerabilityOf(vulnEntry, status) }
            }.distinct()
            .sortedWith(compareBy({ it.id.id }, { it.packages.joinToString(",", transform = Purl::value) }))
    return CycloneDxVexCollection(
        scope = scope,
        vulnerabilities = vulnerabilities,
        productPurls = productPurls,
        skippedEntries =
            statuses.mapNotNull { (vulnEntry, releaseStatuses) ->
                skippedEntry(vulnEntry, releaseStatuses, scope, matching.getValue(vulnEntry))
            },
    )
}

fun cycloneDxVexEmptyReason(
    vulnlogFile: VulnlogFile,
    collection: CycloneDxVexCollection,
): CycloneDxVexEmptyReason {
    val release = vulnlogFile.releases.firstOrNull { release -> release.id == collection.scope.release }
    return when {
        release == null || release.purls.isEmpty() -> CycloneDxVexEmptyReason.RELEASE_DECLARES_NO_PURLS
        collection.productPurls.isEmpty() -> CycloneDxVexEmptyReason.NO_RELEASE_PURL_IN_TAG_SCOPE
        collection.skippedEntries.any {
            it is CycloneDxVexSkippedEntry.NoTags || it is CycloneDxVexSkippedEntry.NoMatchingReleasePurl
        } -> CycloneDxVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS

        collection.scope.tags.isNotEmpty() -> CycloneDxVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE
        else -> CycloneDxVexEmptyReason.NO_ENTRY_ON_RELEASE
    }
}

/** An analysis a revision does not touch keeps the time it was first issued, so an equal one takes it over. */
fun carryOverCycloneDxVexTimes(
    vulnerabilities: List<CycloneDxVexVulnerability>,
    baseline: CycloneDxVexBaseline?,
): List<CycloneDxVexVulnerability> {
    if (baseline == null) return vulnerabilities
    // Keyed as this revision issues a vulnerability, so only an undated one finds a carried time.
    val carried =
        baseline.vulnerabilities
            .mapNotNull { vulnerability ->
                (vulnerability.firstIssued as? VexStatementTime.Carried)?.let { time ->
                    vulnerability.copy(firstIssued = VexStatementTime.Issued) to time
                }
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, times) -> times.minBy(VexStatementTime.Carried::at) }
    return vulnerabilities.map { vulnerability ->
        carried[vulnerability]?.let { time -> vulnerability.copy(firstIssued = time) }
            ?: vulnerability
    }
}

private fun CycloneDxVexScope.coversAnyOf(tags: List<Tag>): Boolean =
    this.tags.isEmpty() || tags.any { it in this.tags }

private fun scopedPurls(
    purls: List<PurlEntry>,
    scope: CycloneDxVexScope,
): List<Purl> =
    purls
        .filter { purlEntry -> scope.coversAnyOf(purlEntry.tags) }
        .map(PurlEntry::purl)
        .sortedBy(Purl::value)

private fun statusOn(
    releaseStatuses: List<ReleaseStatus>,
    scope: CycloneDxVexScope,
): ReleaseStatus? = releaseStatuses.firstOrNull { releaseStatus -> releaseStatus.release == scope.release }

private fun vulnerabilityOf(
    vulnEntry: VulnerabilityEntry,
    releaseStatus: ReleaseStatus,
): CycloneDxVexVulnerability =
    CycloneDxVexVulnerability(
        id = vulnEntry.id,
        aliases = vulnEntry.aliases.sortedBy(VulnId::id),
        description = vulnEntry.description,
        packages = vulnEntry.packages.sortedBy(Purl::value),
        status = releaseStatus.status,
        firstIssued = releaseStatus.since?.let(VexStatementTime::Stated) ?: VexStatementTime.Issued,
    )

private fun skippedEntry(
    vulnEntry: VulnerabilityEntry,
    releaseStatuses: List<ReleaseStatus>,
    scope: CycloneDxVexScope,
    matchingPurls: List<Purl>,
): CycloneDxVexSkippedEntry? =
    when {
        releaseStatuses.isEmpty() -> CycloneDxVexSkippedEntry.NoRelease(vulnEntry.id)
        statusOn(releaseStatuses, scope) == null -> CycloneDxVexSkippedEntry.NotOnRelease(vulnEntry.id)
        matchingPurls.isNotEmpty() -> null
        vulnEntry.tags.isEmpty() -> CycloneDxVexSkippedEntry.NoTags(vulnEntry.id)
        else -> CycloneDxVexSkippedEntry.NoMatchingReleasePurl(vulnEntry.id)
    }
