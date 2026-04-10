package dev.vulnlog.cli.parse.reporting.dto

data class ReportDataDto(
    val project: ProjectDataDto,
    val generatedAt: String,
    val entries: List<ReportEntryDataDto>,
)
