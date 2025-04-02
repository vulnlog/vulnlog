@file:Suppress("TooManyFunctions")

package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VulnerabilityData
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogFixExecution
import dev.vulnlog.dsl.VulnlogReportData
import dev.vulnlog.dsl.VulnlogSuppressPermanentExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilNextPublicationExecution
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dslinterpreter.impl.DefaultReleaseBranchDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogExecutionDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogReportDataImpl
import dev.vulnlog.dslinterpreter.impl.VulnlogTaskDataImpl
import dev.vulnlog.dslinterpreter.service.BranchToInvolvedVersions

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
                    val filteredInvolvedReleaseVersion =
                        filterInvolvedReleaseVersions(filteredReport, vulnerability.involvedReleaseVersions)
                    vulnerability.copy(
                        reportData = filteredReport,
                        taskData = filteredTask,
                        executionData = filteredExecution,
                        involvedReleaseVersions = filteredInvolvedReleaseVersion,
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
): VulnlogExecutionData? =
    executionData?.let {
        val filteredOnReleaseBranch =
            executionData.executions
                .map { a -> a.releases.filter { rb -> rb == releaseBranch }.associateBy { a } }
                .flatMap { it.entries }
                .groupBy { it.key }
                .mapValues { entry -> entry.value.map { it.value }.first() }
                .map(::createVulnlogExecution)
        (executionData as VulnlogExecutionDataImpl).copy(executions = filteredOnReleaseBranch)
    }

private fun createVulnlogExecution(entry: Map.Entry<VulnlogExecution, ReleaseBranchData>): VulnlogExecution {
    val key = entry.key
    val releases = listOf(entry.value)
    return when (key) {
        is VulnlogFixExecution -> key.copy(releases = releases)
        is VulnlogSuppressPermanentExecution -> key.copy(releases = releases)
        is VulnlogSuppressUntilExecution -> key.copy(releases = releases)
        is VulnlogSuppressUntilNextPublicationExecution -> {
            val release = key.releases.first()
            val involvedReleaseVersion = key.involved[releases.first()]!!
            val involved = mapOf(release to involvedReleaseVersion)
            key.copy(releases = releases, involved = involved)
        }
    }
}

fun filterInvolvedReleaseVersions(
    filteredReport: VulnlogReportData?,
    involvedReleaseVersions: BranchToInvolvedVersions?,
): BranchToInvolvedVersions {
    return if (involvedReleaseVersions == null ||
        filteredReport?.affected?.first() == null ||
        involvedReleaseVersions[filteredReport.affected.first()] == null
    ) {
        emptyMap()
    } else {
        mapOf(filteredReport.affected.first() to involvedReleaseVersions[filteredReport.affected.first()]!!)
    }
}
