package dev.vulnlog.cli.parse.reporting.dto

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
