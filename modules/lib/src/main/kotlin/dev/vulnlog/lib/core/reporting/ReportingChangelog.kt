// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.severityOrder
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.ChangelogSummary
import dev.vulnlog.lib.model.reporting.ReportingChangelogEntry
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease

/** A vulnerability paired with the resolution that put it into the changelog. */
internal data class FixedVulnerability(
    val vulnerability: VulnerabilityEntry,
    val resolution: Resolution,
)

/** Collects what every release fixed, newest release first. */
fun collectChangelogReleases(files: List<VulnlogFile>): List<ReportingChangelogRelease> {
    val declared: List<ReleaseEntry> = declaredReleases(files)
    val declaration: Map<Release, ReleaseEntry> = declared.associateBy { it.id }
    val oldestFirst: List<Release> = declared.map { it.id }

    return selectFixedVulnerabilities(files, oldestFirst)
        .groupBy { fixed -> fixed.resolution.release }
        .map { (release, fixed) -> changelogRelease(release, declaration[release], fixed) }
        .sortedByDescending { release -> oldestFirst.indexOf(release.fixedIn) }
}

/** Every release the files declare, in declaration order, each one once. */
internal fun declaredReleases(files: List<VulnlogFile>): List<ReleaseEntry> =
    files.flatMap { file -> file.releases }.distinctBy { it.id }

/** The vulnerabilities whose fix reached users, paired with the resolution that shipped it. */
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
): ReportingChangelogRelease {
    val entries = mergeChangelogEntries(fixed.map(::changelogEntry)).sortedWith(entryOrder)
    return ReportingChangelogRelease(
        fixedIn = release,
        publishedAt = declaration?.publicationDate,
        summary = summarize(entries),
        entries = entries,
    )
}

private fun changelogEntry(fixed: FixedVulnerability): ReportingChangelogEntry =
    ReportingChangelogEntry(
        primaryId = fixed.vulnerability.id,
        aliases = fixed.vulnerability.aliases.toSet(),
        name = fixed.vulnerability.name,
        description = fixed.vulnerability.description,
        impact = defineImpact(fixed.vulnerability),
        note = fixed.resolution.note,
        ref = fixed.resolution.ref,
    )

/** Folds the records of one vulnerability from several files into one entry, keeping every alias. */
internal fun mergeChangelogEntries(entries: List<ReportingChangelogEntry>): List<ReportingChangelogEntry> =
    entries
        .groupBy { it.primaryId }
        .map { (_, group) -> group.reduce(::mergeTwoChangelogEntries) }

private fun mergeTwoChangelogEntries(
    a: ReportingChangelogEntry,
    b: ReportingChangelogEntry,
): ReportingChangelogEntry =
    a.copy(
        aliases = a.aliases + b.aliases,
        name = a.name ?: b.name,
        description = a.description ?: b.description,
        note = a.note ?: b.note,
        ref = a.ref ?: b.ref,
    )

internal fun summarize(entries: List<ReportingChangelogEntry>): ChangelogSummary =
    ChangelogSummary(
        total = entries.size,
        bySeverity =
            entries
                .mapNotNull { severityOf(it.impact) }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(compareBy(::severityOrder)),
    )

private val entryOrder: Comparator<ReportingChangelogEntry> =
    compareBy<ReportingChangelogEntry> { severityOrder(severityOf(it.impact)) }
        .thenBy { it.primaryId.id }
