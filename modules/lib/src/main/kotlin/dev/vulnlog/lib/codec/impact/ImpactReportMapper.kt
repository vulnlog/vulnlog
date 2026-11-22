// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.impact.dto.FilterDto
import dev.vulnlog.lib.codec.impact.dto.ImpactEntryDto
import dev.vulnlog.lib.codec.impact.dto.ImpactReportDto
import dev.vulnlog.lib.codec.impact.dto.ProjectDto
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.core.reporting.severityOf
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry

private const val FORMAT_VERSION = 1

internal object ImpactReportMapper {
    fun toDto(report: ImpactReport): ImpactReportDto =
        ImpactReportDto(
            formatVersion = FORMAT_VERSION,
            project =
                ProjectDto(
                    organization = report.project.organization,
                    name = report.project.name,
                    author = report.project.author,
                ),
            generatedAt = report.generatedAt.toString(),
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
        )

    private fun toImpactEntryDto(entry: ImpactEntry): ImpactEntryDto =
        ImpactEntryDto(
            primaryId = entry.primaryId.id,
            aliases = entry.aliases.map { it.id },
            state = entry.state.canonical(),
            verdict = verdictLabel(entry.impact),
            severity = severityLabel(entry.impact),
            disposition = entry.disposition?.let { canonical(it) },
            verdictDetail = verdictDetail(entry.impact),
            shortDescription = entry.shortDescription,
            analysis = entry.analysis,
            releases = entry.reportFor.map { it.value },
            fixedIn = entry.fixedIn.map { it.value },
        )

    // Null while untriaged: the state column already reads "Investigating".
    private fun verdictLabel(impact: Impact): String? =
        when (impact) {
            is Impact.Affected -> "affected"
            is Impact.NotAffected -> "not affected"
            is Impact.Unknown -> null
        }

    private fun severityLabel(impact: Impact): String? = severityOf(impact)?.let(::canonical)

    private fun verdictDetail(impact: Impact): String? =
        when (impact) {
            is Impact.NotAffected -> impact.reason
            is Impact.Affected -> null
            is Impact.Unknown -> null
        }
}
