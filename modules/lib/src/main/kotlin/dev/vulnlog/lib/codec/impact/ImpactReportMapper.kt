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
import dev.vulnlog.lib.core.severityOrder
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState

internal object ImpactReportMapper {
    fun toDto(report: ImpactReport): ImpactReportDto =
        ImpactReportDto(
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
            entries = report.entries.sortedWith(entrySortComparator).map(::toImpactEntryDto),
        )

    /** In the order Vulnlog declares the values, not the order requested, so equal filters read the same. */
    private fun toFilterDto(
        filter: ResolvedFilter,
        asOf: Release?,
    ): FilterDto =
        FilterDto(
            asOf = asOf?.value,
            tags = filter.tags.map { it.value }.sorted(),
            reporter = filter.reporter?.canonical(),
            states = WorkState.entries.filter { it in filter.states }.map { it.canonical() },
            verdicts = VerdictKind.entries.filter { it in filter.verdicts }.map { it.canonical() },
            dispositions = Disposition.entries.filter { it in filter.dispositions }.map { canonical(it) },
        )

    private val entrySortComparator: Comparator<ImpactEntry> =
        compareBy<ImpactEntry> { stateOrder(it.state) }
            .thenBy { severityOrder(severityOf(it.impact)) }
            .thenBy { it.primaryId.id }

    private fun toImpactEntryDto(entry: ImpactEntry): ImpactEntryDto =
        ImpactEntryDto(
            primaryId = entry.primaryId.id,
            ids = entry.ids.map { it.id },
            state = entry.state.name.lowercase(),
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

    // Actionable first, unknown-is-work second, then live risk, then the two nothing-to-do states.
    private fun stateOrder(state: WorkState): Int =
        when (state) {
            WorkState.OPEN -> 0
            WorkState.UNDER_INVESTIGATION -> 1
            WorkState.ACCEPTED -> 2
            WorkState.RESOLVED -> 3
            WorkState.NOT_APPLICABLE -> 4
        }
}
