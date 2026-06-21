// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.WorkState
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability

/**
 * Collects and filters suppressed vulnerabilities from a given Vulnlog file based on the specified suppression
 * filter criteria. The vulnerabilities are grouped by their reporter type.
 *
 * @param vulnlogFile The Vulnlog file containing vulnerability records to analyze.
 * @param filter The suppression filter to apply for selecting and grouping vulnerabilities.
 * @return A map where the keys are the reporter types, and the values are lists of suppressed vulnerabilities
 *         associated with each reporter.
 */
fun collectSuppressedVulnerabilities(
    vulnlogFile: VulnlogFile,
    filter: SuppressionFilter,
): Map<ReporterType, List<SuppressedVulnerability>> =
    vulnlogFile.vulnerabilities
        .asSequence()
        .filterNot { vulnerability -> isResolved(vulnerability, filter.filter.releases) }
        .flatMap(::explodeOnReports)
        .applyFilter(filter)
        .groupBy { it.reporter }

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
