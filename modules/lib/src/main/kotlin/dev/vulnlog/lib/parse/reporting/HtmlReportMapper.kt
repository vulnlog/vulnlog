// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.reporting

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.report.Impact
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.model.report.WorkState
import dev.vulnlog.lib.parse.reporting.dto.FilterDataDto
import dev.vulnlog.lib.parse.reporting.dto.ProjectDataDto
import dev.vulnlog.lib.parse.reporting.dto.ReportDataDto
import dev.vulnlog.lib.parse.reporting.dto.ReportEntryDataDto
import java.time.Instant

object HtmlReportMapper {
    fun toDto(
        project: Project,
        entries: List<ReportingEntry>,
        generatedAt: Instant,
        vulnlogVersion: String,
        inputs: List<String>,
        filter: FilterDataDto,
    ): ReportDataDto =
        ReportDataDto(
            project =
                ProjectDataDto(
                    organization = project.organization,
                    name = project.name,
                    author = project.author,
                ),
            generatedAt = generatedAt.toString(),
            vulnlogVersion = vulnlogVersion,
            inputs = inputs,
            filter = filter,
            entries = entries.sortedWith(entrySortComparator).map(::toReportEntryData),
        )

    private val entrySortComparator: Comparator<ReportingEntry> =
        compareBy<ReportingEntry> { stateOrder(it.state) }
            .thenBy { severityOrder(severityOf(it.impact)) }
            .thenBy { it.primaryId.id }

    private fun toReportEntryData(entry: ReportingEntry): ReportEntryDataDto =
        ReportEntryDataDto(
            primaryId = entry.primaryId.id,
            ids = entry.ids.map { it.id },
            state = entry.state.name.lowercase(),
            verdict = verdictLabel(entry.impact),
            severity = severityLabel(entry.impact),
            verdictDetail = verdictDetail(entry.impact),
            shortDescription = entry.shortDescription,
            analysis = entry.analysis,
            releases = entry.reportFor.map { it.value },
            fixedIn = entry.fixedIn.map { it.value },
        )

    private fun verdictLabel(impact: Impact): String =
        when (impact) {
            is Impact.Affected -> "affected"
            is Impact.NotAffected -> "not affected"
            is Impact.AcceptableRisk -> "risk acceptable"
            is Impact.Unknown -> "under_investigation"
        }

    private fun severityOf(impact: Impact): Severity? =
        when (impact) {
            is Impact.Affected -> impact.severity
            is Impact.AcceptableRisk -> impact.severity
            is Impact.NotAffected -> null
            is Impact.Unknown -> null
        }

    private fun severityLabel(impact: Impact): String? = severityOf(impact)?.name?.lowercase()

    private fun verdictDetail(impact: Impact): String? =
        when (impact) {
            is Impact.NotAffected -> impact.reason
            is Impact.AcceptableRisk -> null
            is Impact.Affected -> null
            is Impact.Unknown -> null
        }

    private fun stateOrder(state: WorkState): Int =
        when (state) {
            WorkState.OPEN -> 0
            WorkState.UNDER_INVESTIGATION -> 1
            WorkState.RESOLVED -> 2
            WorkState.DISMISSED -> 3
        }

    private fun severityOrder(severity: Severity?): Int =
        when (severity) {
            Severity.CRITICAL -> 0
            Severity.HIGH -> 1
            Severity.MEDIUM -> 2
            Severity.LOW -> 3
            null -> 4
        }
}
