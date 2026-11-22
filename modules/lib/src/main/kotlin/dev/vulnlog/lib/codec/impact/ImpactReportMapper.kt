// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.impact.dto.FilterDto
import dev.vulnlog.lib.codec.impact.dto.ImpactEntryDto
import dev.vulnlog.lib.codec.impact.dto.ImpactReportDto
import dev.vulnlog.lib.codec.report.formatGeneratedAt
import dev.vulnlog.lib.codec.report.labelJustification
import dev.vulnlog.lib.codec.report.labelSeverity
import dev.vulnlog.lib.codec.report.labelVerdict
import dev.vulnlog.lib.codec.report.sortInDeclaredOrder
import dev.vulnlog.lib.codec.report.sortTagValues
import dev.vulnlog.lib.codec.report.sortVulnIdValues
import dev.vulnlog.lib.codec.report.toProjectDto
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.ImpactEntry

private const val FORMAT_VERSION = 1

internal object ImpactReportMapper {
    fun toDto(report: ImpactReport): ImpactReportDto =
        ImpactReportDto(
            formatVersion = FORMAT_VERSION,
            project = toProjectDto(report.project),
            generatedAt = formatGeneratedAt(report.generatedAt),
            vulnlogVersion = report.vulnlogVersion,
            inputs = report.inputs,
            filter = toFilterDto(report.filter, report.asOf),
            entries = report.entries.sortedWith(impactEntryOrder).map(::toImpactEntryDto),
        )

    private fun toFilterDto(
        filter: ResolvedFilter,
        asOf: Release?,
    ): FilterDto =
        FilterDto(
            asOf = asOf?.value,
            tags = sortTagValues(filter),
            reporter = filter.reporter?.canonical(),
            states = sortInDeclaredOrder(filter.states).map { it.canonical() },
            verdicts = sortInDeclaredOrder(filter.verdicts).map { it.canonical() },
            dispositions = sortInDeclaredOrder(filter.dispositions).map { canonical(it) },
            vulnIds = sortVulnIdValues(filter),
        )

    private fun toImpactEntryDto(entry: ImpactEntry): ImpactEntryDto =
        ImpactEntryDto(
            primaryId = entry.primaryId.id,
            aliases = entry.aliases.map { it.id },
            state = entry.state.canonical(),
            verdict = labelVerdict(entry.impact),
            severity = labelSeverity(entry.impact),
            disposition = entry.disposition?.let { canonical(it) },
            justification = labelJustification(entry.impact),
            description = entry.shortDescription,
            analysis = entry.analysis,
            releases = entry.reportFor.map { it.value },
            fixedIn = entry.fixedIn.map { it.value },
        )
}
