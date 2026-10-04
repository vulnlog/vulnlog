// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.severityOrder
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.ChangelogEntry
import dev.vulnlog.lib.model.reporting.ChangelogRelease
import dev.vulnlog.lib.model.reporting.ChangelogSummary

internal data class FixedVulnerability(
    val vulnerability: VulnerabilityEntry,
    val resolution: Resolution,
)

/** Newest release first, as a changelog reads. */
fun collectChangelogReleases(files: List<VulnlogFile>): List<ChangelogRelease> {
    val declared: List<ReleaseEntry> = declaredReleases(files)
    val declaration: Map<Release, ReleaseEntry> = declared.associateBy { it.id }
    val oldestFirst: List<Release> = declared.map { it.id }

    return selectFixedVulnerabilities(files, oldestFirst)
        .groupBy { fixed -> fixed.resolution.release }
        .map { (release, fixed) -> changelogRelease(release, declaration[release], fixed) }
        .sortedByDescending { release -> oldestFirst.indexOf(release.fixedIn) }
}

/** Declaration order is release order: validation warns when a file does not declare its releases oldest first. */
internal fun declaredReleases(files: List<VulnlogFile>): List<ReleaseEntry> =
    files.flatMap { file -> file.releases }.distinctBy { it.id }

internal fun selectFixedVulnerabilities(
    files: List<VulnlogFile>,
    oldestFirst: List<Release>,
): List<FixedVulnerability> =
    files
        .asSequence()
        .flatMap { file -> file.vulnerabilities }
        .mapNotNull { vuln -> vuln.resolution?.let { FixedVulnerability(vuln, it) } }
        .filter { fixed -> shippedVulnerable(fixed, oldestFirst) }
        .toList()

/**
 * Whether users ever ran a release containing the vulnerability.
 *
 * They did once it was reported for a release declared before the one that fixed it. A
 * vulnerability reported only for the fix release, or only for releases after it, exposed nobody
 * and so has nothing to announce. A vulnerability carried by an earlier release still counts even
 * when the fix release is among the releases it was reported for.
 */
private fun shippedVulnerable(
    fixed: FixedVulnerability,
    oldestFirst: List<Release>,
): Boolean {
    val fixedAt = oldestFirst.indexOf(fixed.resolution.release)
    return fixed.vulnerability.releases.any { reportedFor ->
        val reportedAt = oldestFirst.indexOf(reportedFor)
        reportedAt in 0..<fixedAt
    }
}

private fun changelogRelease(
    release: Release,
    declaration: ReleaseEntry?,
    fixed: List<FixedVulnerability>,
): ChangelogRelease {
    val entries = mergeChangelogEntries(fixed.map(::changelogEntry)).sortedWith(entryOrder)
    return ChangelogRelease(
        fixedIn = release,
        publishedAt = declaration?.publicationDate,
        summary = summarize(entries),
        entries = entries,
    )
}

private fun changelogEntry(fixed: FixedVulnerability): ChangelogEntry =
    ChangelogEntry(
        primaryId = fixed.vulnerability.id,
        aliases = fixed.vulnerability.aliases.toSet(),
        name = fixed.vulnerability.name,
        description = fixed.vulnerability.description,
        impact = defineImpact(fixed.vulnerability),
        note = fixed.resolution.note,
        ref = fixed.resolution.ref,
    )

internal fun mergeChangelogEntries(entries: List<ChangelogEntry>): List<ChangelogEntry> =
    entries
        .groupBy { it.primaryId }
        .map { (_, group) -> group.reduce(::mergeTwoChangelogEntries) }

private fun mergeTwoChangelogEntries(
    a: ChangelogEntry,
    b: ChangelogEntry,
): ChangelogEntry =
    a.copy(
        aliases = a.aliases + b.aliases,
        name = a.name ?: b.name,
        description = a.description ?: b.description,
        note = a.note ?: b.note,
        ref = a.ref ?: b.ref,
    )

fun summarize(entries: List<ChangelogEntry>): ChangelogSummary =
    ChangelogSummary(
        total = entries.size,
        bySeverity =
            entries
                .mapNotNull { severityOf(it.impact) }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(compareBy(::severityOrder)),
    )

private val entryOrder: Comparator<ChangelogEntry> =
    compareBy<ChangelogEntry> { severityOrder(severityOf(it.impact)) }
        .thenBy { it.primaryId.id }
