// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import dev.vulnlog.lib.shell.SuppressionFormatRequest

/**
 * Builds the per-reporter suppression outputs for the given target reporters, applying the requested
 * output format. Reporters without a suppressible format (such as [ReporterType.OTHER]) are skipped.
 *
 * @param targetReporters The reporters to generate suppression outputs for.
 * @param reporterToSuppressions The suppressed vulnerabilities grouped by reporter.
 * @param formatRequest The requested output format. Defaults to [SuppressionFormatRequest.Auto].
 * @return A set of [SuppressionOutput] objects, one per suppressible target reporter.
 */
fun buildSuppressionOutputs(
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
        fileName = format.reporter.canonical() + ".generic.json",
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
