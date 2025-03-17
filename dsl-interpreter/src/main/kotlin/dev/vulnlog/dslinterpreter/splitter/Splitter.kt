@file:Suppress("TooManyFunctions")

package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VulnerabilityData
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogReportData
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dslinterpreter.impl.DefaultReleaseBranchDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogExecutionDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogReportDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogTaskDataImpl

fun vulnerabilityPerBranch(
    releases: Set<ReleaseBranchData>,
    vulnerabilities: List<VulnerabilityData>,
): Map<ReleaseBranchData, List<VulnerabilityData>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else if (releases.isEmpty()) {
        mapOf(DefaultReleaseBranchDataImpl to vulnerabilities)
    } else {
        splitAndGroupByBranch(vulnerabilities)
    }
}

private fun splitAndGroupByBranch(
    vulnerabilities: List<VulnerabilityData>,
): Map<ReleaseBranchData, List<VulnerabilityData>> {
    val splitVulnerabilities: Map<ReleaseBranchData, List<VulnerabilityData>> =
        vulnerabilities.map { vulnerability ->
            val affectedReleaseBranches = vulnerability.reportData?.affected ?: emptyList()
            val splitVulnerabilities: Map<ReleaseBranchData, VulnerabilityData> =
                affectedReleaseBranches.associateWith { releaseBranch ->
                    val filteredReport = filterOnReleaseBranch(releaseBranch, vulnerability.reportData)
                    val filteredTask = filterOnReleaseBranch(releaseBranch, vulnerability.taskData)
                    val filteredExecution = filterOnReleaseBranch(releaseBranch, vulnerability.executionData)
                    vulnerability.copy(
                        reportData = filteredReport,
                        taskData = filteredTask,
                        executionData = filteredExecution,
                    )
                }
            splitVulnerabilities
        }
            .filter { it.isNotEmpty() }
            .flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
    return splitVulnerabilities
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    reportData: VulnlogReportData?,
): VulnlogReportData? {
    return if (reportData == null) {
        null
    } else {
        (reportData as VulnlogReportDataImpl).copy(affected = reportData.affected.filter { it == releaseBranch })
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    taskData: VulnlogTaskData?,
): VulnlogTaskData? {
    return if (taskData == null) {
        null
    } else {
        val filteredOnReleaseBranch =
            taskData.taskOnReleaseBranch.entries
                .associate { it.key to it.value.filter { rb -> rb == releaseBranch } }
                .filter { it.value.isNotEmpty() }
        (taskData as VulnlogTaskDataImpl).copy(taskOnReleaseBranch = filteredOnReleaseBranch)
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    executionData: VulnlogExecutionData?,
): VulnlogExecutionData? {
    return if (executionData == null) {
        null
    } else {
        val filteredOnReleaseBranch = executionData.executions.filter { it.releases.contains(releaseBranch) }
        (executionData as VulnlogExecutionDataImpl).copy(executions = filteredOnReleaseBranch)
    }
}
