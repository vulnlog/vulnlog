// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.suppression

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.scopeResolution
import dev.vulnlog.lib.core.reporting.findWorkState
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.WorkState
import dev.vulnlog.lib.model.suppression.SuppressedVulnerability
import dev.vulnlog.lib.model.suppression.SuppressionCollection
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import java.time.LocalDate

/**
 * Resolved vulnerabilities and expired suppressions come back as exclusions, so a message can explain them. Entries
 * outside the requested releases, tags or reporter do not: the user left them out.
 */
fun collectSuppressedVulnerabilities(
    vulnlogFile: VulnlogFile,
    filter: ResolvedFilter,
    today: LocalDate,
): SuppressionCollection {
    val (resolved, unresolved) =
        vulnlogFile.vulnerabilities.partition { vulnerability -> isResolved(vulnerability, filter.releases) }
    val (active, expired) =
        unresolved
            .asSequence()
            .flatMap(::explodeOnReports)
            .filter { suppression -> isInScope(suppression, filter) }
            .partition { it.isActiveOn(today) }
    val resolvedExclusions =
        resolved
            .asSequence()
            .flatMap(::explodeOnReports)
            .filter { suppression -> isInScope(suppression, filter) }
            .map { SuppressionExclusion.Resolved(it.id) }
            .toList()
    return SuppressionCollection(
        included = active.groupBy { it.reporter },
        exclusions = (resolvedExclusions + expired.mapNotNull(::expiredExclusion)).distinct(),
    )
}

private fun isInScope(
    suppression: SuppressedVulnerability,
    filter: ResolvedFilter,
): Boolean =
    (filter.releases.isEmpty() || suppression.releases.any { release -> release in filter.releases }) &&
        (filter.tags.isEmpty() || filter.tags.any { tag -> tag in suppression.tags }) &&
        (filter.reporter == null || filter.reporter == suppression.reporter)

private fun expiredExclusion(suppression: SuppressedVulnerability): SuppressionExclusion? =
    suppression.expiresAt?.let { expiredAt ->
        SuppressionExclusion.Expired(suppression.id, suppression.reporter, expiredAt)
    }

private fun isResolved(
    vulnEntry: VulnerabilityEntry,
    filterReleases: Set<Release>,
): Boolean = findWorkState(scopeResolution(vulnEntry, filterReleases)) == WorkState.RESOLVED

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
