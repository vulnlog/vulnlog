// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.WorkState
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.result.SuppressionCollectionResult
import dev.vulnlog.lib.result.SuppressionExclusion

/**
 * Collects and filters suppressed vulnerabilities from a given Vulnlog file based on the specified suppression
 * filter criteria. The collected vulnerabilities are grouped by their reporter type. Resolved vulnerabilities
 * and expired suppressions come back as exclusions; entries that merely fall outside the user-requested
 * release, tag, or reporter filter do not.
 *
 * @param vulnlogFile The Vulnlog file containing vulnerability records to analyze.
 * @param filter The suppression filter to apply for selecting and grouping vulnerabilities.
 * @return The suppressed vulnerabilities grouped by reporter type, plus all exclusions.
 */
fun collectSuppressedVulnerabilities(
    vulnlogFile: VulnlogFile,
    filter: SuppressionFilter,
): SuppressionCollectionResult {
    val (resolved, unresolved) =
        vulnlogFile.vulnerabilities.partition { vulnerability -> isResolved(vulnerability, filter.filter.releases) }
    val (active, expired) =
        unresolved
            .asSequence()
            .flatMap(::explodeOnReports)
            .applyFilter(filter.filter)
            .partition { it.isActiveOn(filter.today) }
    val resolvedExclusions =
        resolved
            .asSequence()
            .flatMap(::explodeOnReports)
            .applyFilter(filter.filter)
            .map { SuppressionExclusion.ResolvedVulnerability(it.id) }
            .toList()
    return SuppressionCollectionResult(
        included = active.groupBy { it.reporter },
        exclusions = (resolvedExclusions + expired.mapNotNull(::expiredExclusion)).distinct(),
    )
}

private fun expiredExclusion(suppression: SuppressedVulnerability): SuppressionExclusion? =
    suppression.expiresAt?.let { expiredAt ->
        SuppressionExclusion.ExpiredSuppression(suppression.id, suppression.reporter, expiredAt)
    }

private fun isResolved(
    vulnEntry: VulnerabilityEntry,
    filterReleases: Set<Release>,
): Boolean = findWorkState(vulnEntry, filterReleases) == WorkState.RESOLVED

private fun explodeOnReports(vulnerability: VulnerabilityEntry): List<SuppressedVulnerability> =
    vulnerability.reports
        .filter { report -> report.suppress != null || vulnerability.verdict is Verdict.NotAffected }
        .flatMap { report -> explodeOnVulnIds(report, vulnerability) }

private fun explodeOnVulnIds(
    report: ReportEntry,
    vulnerability: VulnerabilityEntry,
): List<SuppressedVulnerability> {
    val vulnIds = report.vulnIds.ifEmpty { setOf(vulnerability.id) }
    return vulnIds.map { id ->
        SuppressedVulnerability(
            id = id,
            releases = vulnerability.releases,
            reporter = report.reporter,
            expiresAt = report.suppress?.expiresAt,
            tags = vulnerability.tags,
            analysis = vulnerability.analysis ?: "",
        )
    }
}
