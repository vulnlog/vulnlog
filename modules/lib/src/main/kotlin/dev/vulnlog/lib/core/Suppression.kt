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
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import dev.vulnlog.lib.shell.SuppressionFormatRequest

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

/**
 * Maps target reporters to their corresponding suppression outputs based on default settings
 * and the list of suppressed vulnerabilities. Only reporters with suppressible default
 * settings are processed.
 *
 * @param targetReporters A set of `ReporterType` enumeration values representing the reporters
 *                        for which suppression mappings should be generated.
 * @param reporterToSuppressions A map where the keys are `ReporterType` enumeration values
 *                                and the values are lists of `SuppressedVulnerability` objects
 *                                associated with those reporters.
 * @return A set of `SuppressionOutput` objects that represent the suppression configurations
 *         for the specified target reporters.
 */
fun mapToSuppression(
    targetReporters: Set<ReporterType>,
    reporterToSuppressions: Map<ReporterType, List<SuppressedVulnerability>>,
    formatRequest: SuppressionFormatRequest = SuppressionFormatRequest.Auto,
): Set<SuppressionOutput> =
    targetReporters
        .filterNot { reporter -> reporter == ReporterType.OTHER }
        .map { reporter -> reporter to resolveFormatRequest(formatRequest, reporter) }
        .map { (reporter, format) -> createSuppression(format, reporterToSuppressions[reporter]) }
        .toSet()

private fun resolveFormatRequest(
    format: SuppressionFormatRequest,
    reporter: ReporterType,
): SuppressionFormat =
    when (format) {
        SuppressionFormatRequest.Auto -> nativeFormat(reporter) ?: SuppressionFormat.GenericFormat.Generic(reporter)
        SuppressionFormatRequest.Generic -> SuppressionFormat.GenericFormat.Generic(reporter)
    }

private fun nativeFormat(reporter: ReporterType): SuppressionFormat.NativeFormat? =
    when (reporter) {
        ReporterType.CARGO_AUDIT -> SuppressionFormat.NativeFormat.CargoAudit
        ReporterType.SNYK -> SuppressionFormat.NativeFormat.Snyk
        ReporterType.TRIVY -> SuppressionFormat.NativeFormat.Trivy
        else -> null
    }

private fun createSuppression(
    format: SuppressionFormat,
    suppressions: List<SuppressedVulnerability>?,
): SuppressionOutput {
    val suppressionEntries = suppressions ?: emptyList()
    return when (format) {
        is SuppressionFormat.GenericFormat.Generic -> createGenericSuppression(format, suppressionEntries)
        is SuppressionFormat.NativeFormat.Trivy -> createTrivySuppression(format, suppressionEntries)
        is SuppressionFormat.NativeFormat.Snyk -> createSnykSuppression(format, suppressionEntries)
        is SuppressionFormat.NativeFormat.CargoAudit -> createCargoAuditSuppression(format, suppressionEntries)
    }
}

private fun createGenericSuppression(
    format: SuppressionFormat.GenericFormat.Generic,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in format.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.GenericSuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.GenericSuppression(
        fileName = format.reporter.name.lowercase() + ".generic.json",
        entries = entries,
    )
}

private fun createTrivySuppression(
    format: SuppressionFormat.NativeFormat.Trivy,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in format.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.TrivySuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.TrivySuppression(entries = entries)
}

private fun createCargoAuditSuppression(
    format: SuppressionFormat.NativeFormat.CargoAudit,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in format.vulnIdTypes }
            .map { suppression -> SuppressionVuln.CargoAuditSuppressionEntry(id = suppression.id) }
            .toSet()
    return SuppressionOutput.CargoAuditSuppression(entries = entries)
}

private fun createSnykSuppression(
    format: SuppressionFormat.NativeFormat.Snyk,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in format.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.SnykSuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.SnykSuppression(entries = entries)
}
