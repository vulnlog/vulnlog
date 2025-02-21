package dev.vulnlog.dsl

data class VulnlogData(
    val ids: List<String>,
    val reportData: VulnlogReportData = VulnlogReportDataEmpty,
    val analysisData: VulnlogAnalysisData = VulnlogAnalysisDataEmpty,
    val taskData: VulnlogTaskData = VulnlogTaskDataEmpty,
    val executionData: VulnlogExecutionData = VulnlogExecutionDataEmpty,
)
