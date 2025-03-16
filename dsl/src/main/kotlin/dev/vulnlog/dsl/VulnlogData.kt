package dev.vulnlog.dsl

public data class VulnlogData(
    val ids: List<String>,
    val reportData: VulnlogReportData? = null,
    val analysisData: VulnlogAnalysisData = VulnlogAnalysisDataEmpty,
    val taskData: VulnlogTaskData = VulnlogTaskDataEmpty,
    val executionData: VulnlogExecutionData = VulnlogExecutionDataEmpty,
)
