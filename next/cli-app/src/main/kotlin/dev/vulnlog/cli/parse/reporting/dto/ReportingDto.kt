package dev.vulnlog.cli.parse.reporting.dto

data class ReportDataDto(
    val project: ProjectDataDto,
    val generatedAt: String,
    val entries: List<ReportEntryDataDto>,
)

data class ProjectDataDto(
    val organization: String,
    val name: String,
    val author: String,
)

data class ReportEntryDataDto(
    val primaryId: String,
    val ids: List<String>,
    val state: String,
    val verdict: String,
    val severity: String?,
    val verdictDetail: String?,
    val shortDescription: String?,
    val analysis: String?,
    val releases: List<String>,
    val fixedIn: List<String>,
)
