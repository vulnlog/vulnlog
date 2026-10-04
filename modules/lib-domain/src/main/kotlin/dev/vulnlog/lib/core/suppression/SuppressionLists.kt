// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.suppression

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.suppression.SuppressedVulnerability
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import dev.vulnlog.lib.model.suppression.SuppressionLists

fun nativeSuppressionFormat(reporter: ReporterType): SuppressionFormat? =
    when (reporter) {
        ReporterType.TRIVY -> SuppressionFormat.Trivy
        ReporterType.SNYK -> SuppressionFormat.Snyk
        ReporterType.CARGO_AUDIT -> SuppressionFormat.CargoAudit
        else -> null
    }

/** None for [ReporterType.OTHER]: it names no scanner, so no tool would read the file. */
fun genericSuppressionFormat(reporter: ReporterType): SuppressionFormat? =
    if (reporter == ReporterType.OTHER) null else SuppressionFormat.Generic(reporter)

/** The name the scanner looks for, so it finds the file without extra configuration. */
fun suppressionFileName(format: SuppressionFormat): String =
    when (format) {
        is SuppressionFormat.Generic -> format.reporter.canonical() + ".generic.json"
        SuppressionFormat.Trivy -> ".trivyignore.yaml"
        SuppressionFormat.Snyk -> ".snyk"
        SuppressionFormat.CargoAudit -> "audit.toml"
    }

/**
 * One list per reporter that has a format, an empty one too, so a regenerated file drops suppressions that no longer
 * apply. The entries of a reporter without a format and the ids a format cannot hold come back as exclusions.
 */
fun buildSuppressionLists(
    included: Map<ReporterType, List<SuppressedVulnerability>>,
    formats: Map<ReporterType, SuppressionFormat?>,
): SuppressionLists {
    val built =
        formats.mapNotNull { (reporter, format) -> format?.let { buildList(it, included[reporter].orEmpty()) } }
    val unsupportedReporters =
        formats
            .filterValues { it == null }
            .keys
            .flatMap { reporter ->
                included[reporter].orEmpty().map { SuppressionExclusion.UnsupportedReporter(it.id, reporter) }
            }
    return SuppressionLists(
        lists = built.map(ListWithExclusions::list),
        exclusions = (built.flatMap(ListWithExclusions::exclusions) + unsupportedReporters).distinct(),
    )
}

private data class ListWithExclusions(
    val list: SuppressionList,
    val exclusions: List<SuppressionExclusion>,
)

private fun buildList(
    format: SuppressionFormat,
    suppressions: List<SuppressedVulnerability>,
): ListWithExclusions {
    val (supported, unsupported) = suppressions.partition { it.id::class in format.vulnIdTypes }
    return ListWithExclusions(
        list = SuppressionList(format, supported.map { entry(format, it) }.toSet()),
        exclusions = unsupported.map { SuppressionExclusion.UnsupportedIdType(it.id, format) },
    )
}

/** cargo-audit ignores bare ids, so its entries keep only the id and collapse to one per id. */
private fun entry(
    format: SuppressionFormat,
    suppression: SuppressedVulnerability,
): SuppressionEntry =
    when (format) {
        SuppressionFormat.CargoAudit -> SuppressionEntry(suppression.id)
        is SuppressionFormat.Generic, SuppressionFormat.Trivy, SuppressionFormat.Snyk ->
            SuppressionEntry(suppression.id, suppression.expiresAt, suppression.analysis)
    }
