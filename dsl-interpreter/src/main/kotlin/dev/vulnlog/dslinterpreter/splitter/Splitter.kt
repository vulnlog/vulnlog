@file:Suppress("TooManyFunctions")

package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.DefaultReleaseBranchDataImpl
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VulnlogData
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogExecutionDataEmpty
import dev.vulnlog.dsl.VulnlogExecutionDataImpl
import dev.vulnlog.dsl.VulnlogReportData
import dev.vulnlog.dsl.VulnlogReportDataEmpty
import dev.vulnlog.dsl.VulnlogReportDataImpl
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dsl.VulnlogTaskDataEmpty
import dev.vulnlog.dsl.VulnlogTaskDataImpl

fun vulnerabilityPerBranch3(
    releases: Set<ReleaseBranchData>,
    vulnerabilities: List<VulnlogData>,
): Map<ReleaseBranchData, List<VulnlogData>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else if (releases.isEmpty()) {
        mapOf(DefaultReleaseBranchDataImpl to vulnerabilities)
    } else {
        splitAndGroupByBranch3(vulnerabilities)
    }
}

private fun splitAndGroupByBranch3(vulnerabilities: List<VulnlogData>): Map<ReleaseBranchData, List<VulnlogData>> {
    val splitVulnerabilities: Map<ReleaseBranchData, List<VulnlogData>> =
        vulnerabilities.map { vulnerability ->
            val affectedReleaseBranches = vulnerability.reportData.affected
            val splitVulnerabilities: Map<ReleaseBranchData, VulnlogData> =
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
    reportData: VulnlogReportData,
): VulnlogReportData {
    return when (reportData) {
        is VulnlogReportDataEmpty -> reportData
        is VulnlogReportDataImpl -> reportData.copy(affected = reportData.affected.filter { it == releaseBranch })
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    taskData: VulnlogTaskData,
): VulnlogTaskData {
    return when (taskData) {
        is VulnlogTaskDataEmpty -> taskData
        is VulnlogTaskDataImpl -> {
            val filteredOnReleaseBranch =
                taskData.taskOnReleaseBranch.entries
                    .associate { it.key to it.value.filter { rb -> rb == releaseBranch } }
                    .filter { it.value.isNotEmpty() }
            taskData.copy(taskOnReleaseBranch = filteredOnReleaseBranch)
        }
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    executionData: VulnlogExecutionData,
): VulnlogExecutionData {
    return when (executionData) {
        is VulnlogExecutionDataEmpty -> executionData
        is VulnlogExecutionDataImpl -> {
            val filteredOnReleaseBranch = executionData.tasks.filter { it.releases.contains(releaseBranch) }
            executionData.copy(tasks = filteredOnReleaseBranch)
        }
    }
}
