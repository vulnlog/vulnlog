package dev.vulnlog.dsl

public data class VulnlogData(
    val ids: List<String>,
    val reportData: VulnlogReportData? = null,
    val analysisData: VulnlogAnalysisData? = null,
    val taskData: VulnlogTaskData = VulnlogTaskDataEmpty,
    val executionData: VulnlogExecutionData = VulnlogExecutionDataEmpty,
)
