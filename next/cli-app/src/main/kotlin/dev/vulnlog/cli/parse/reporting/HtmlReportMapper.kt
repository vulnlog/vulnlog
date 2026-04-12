package dev.vulnlog.cli.parse.reporting

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.report.Impact
import dev.vulnlog.cli.model.report.ReportingEntry
import dev.vulnlog.cli.parse.reporting.dto.ProjectDataDto
import dev.vulnlog.cli.parse.reporting.dto.ReportDataDto
import dev.vulnlog.cli.parse.reporting.dto.ReportEntryDataDto
import java.time.LocalDate

object HtmlReportMapper {
    fun toDto(
        project: Project,
        entries: List<ReportingEntry>,
        generatedAt: LocalDate,
    ): ReportDataDto =
        ReportDataDto(
            project =
                ProjectDataDto(
                    organization = project.organization,
                    name = project.name,
                    author = project.author,
                ),
            generatedAt = generatedAt.toString(),
            entries = entries.map(::toReportEntryData),
        )

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

    private fun severityLabel(impact: Impact): String? =
        when (impact) {
            is Impact.Affected -> impact.severity.name.lowercase()
            is Impact.AcceptableRisk -> impact.severity.name.lowercase()
            is Impact.NotAffected -> null
            is Impact.Unknown -> null
        }

    private fun verdictDetail(impact: Impact): String? =
        when (impact) {
            is Impact.NotAffected -> impact.reason
            is Impact.AcceptableRisk -> impact.severity.name.lowercase()
            is Impact.Affected -> impact.severity.name.lowercase()
            is Impact.Unknown -> null
        }
}
