package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.ExecutionPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.TaskDataPerBranch
import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.VulnlogAnalysisData
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogFixExecution
import dev.vulnlog.dsl.VulnlogReportData
import dev.vulnlog.dsl.VulnlogSuppressPermanentExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilNextPublicationExecution
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dslinterpreter.impl.InvolvedReleaseVersionImpl
import dev.vulnlog.dslinterpreter.repository.VulnerabilityDataRepository
import dev.vulnlog.dslinterpreter.service.BranchToInvolvedVersions

/**
 * Splits a vulnerability report into distinct reports per affected release branch.
 */
fun vulnerabilityPerBranch(
    vulnerabilities: VulnerabilityDataRepository,
): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else {
        splitAndGroupByBranch(vulnerabilities)
    }
}

private fun splitAndGroupByBranch(
    vulnerabilities: VulnerabilityDataRepository,
): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
    val splitVulnerabilities: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
        vulnerabilities.getVulnerabilities().asSequence().map { vulnerability ->
            val affectedReleaseBranches = vulnerability.reportData?.affected ?: emptyList()
            val splitVulnerabilities: Map<ReleaseBranchData, VulnerabilityDataPerBranch?> =
                affectedReleaseBranches.associateWith { releaseBranch ->
                    val filteredReport = filterOnReleaseBranch(releaseBranch, vulnerability.reportData)
                    filteredReport?.let { report ->
                        val filteredAnalysis = filterOnReleaseBranch(vulnerability.analysisData)
                        val filteredTask = filterOnReleaseBranch(releaseBranch, vulnerability.taskData)
                        val filteredExecution = filterOnReleaseBranch(releaseBranch, vulnerability.executionData)
                        val filteredInvolvedReleaseVersion =
                            filterInvolvedReleaseVersions(
                                releaseBranch,
                                vulnerability.involvedReleaseVersions,
                                filteredExecution,
                            )
                        VulnerabilityDataPerBranch(
                            branch = releaseBranch,
                            ids = vulnerability.ids,
                            reportData = report,
                            analysisData = filteredAnalysis,
                            taskData = filteredTask,
                            executionData = filteredExecution,
                            involvedReleaseVersions = filteredInvolvedReleaseVersion,
                        )
                    }
                }
            splitVulnerabilities
        }
            .filter { it.isNotEmpty() }
            .flatMap { it.entries }
            .filter { it.value != null }
            .groupBy({ it.key }, { it.value!! })
    return splitVulnerabilities
}

private fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    reportData: VulnlogReportData?,
): ReportDataPerBranch? {
    return if (reportData == null) {
        null
    } else {
        val relevant: List<ReleaseBranchData> = reportData.affected.filter { it == releaseBranch }
        if (relevant.size > 1) {
            error("Multiple vulnerability reports for the same release branch: $releaseBranch")
        } else if (relevant.isEmpty()) {
            null
        } else {
            ReportDataPerBranch(reportData.reporters, reportData.awareAt)
        }
    }
}

fun filterOnReleaseBranch(analysisData: VulnlogAnalysisData?): AnalysisDataPerBranch? {
    return if (analysisData == null) {
        null
    } else {
        return AnalysisDataPerBranch(analysisData.analysedAt, analysisData.verdict, analysisData.reasoning)
    }
}

private fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    taskData: VulnlogTaskData?,
): TaskDataPerBranch? {
    return if (taskData == null) {
        null
    } else {
        val filteredOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>> =
            taskData.taskOnReleaseBranch.entries
                .associate { it.key to it.value.filter { rb -> rb == releaseBranch } }
                .filter { it.value.isNotEmpty() }
        if (filteredOnReleaseBranch.keys.size > 1) {
            error("Multiple task actions for the same release branch are currently not supported")
        } else if (filteredOnReleaseBranch.isEmpty() || filteredOnReleaseBranch.keys.isEmpty()) {
            null
        } else {
            TaskDataPerBranch(filteredOnReleaseBranch.keys.first())
        }
    }
}

private fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    executionData: VulnlogExecutionData?,
): ExecutionDataPerBranch? =
    executionData?.let {
        val filteredOnReleaseBranch =
            executionData.executions
                .map { a -> a.releases.filter { rb -> rb == releaseBranch }.associateBy { a } }
                .flatMap { it.entries }
                .groupBy { it.key }
                .mapValues { entry -> entry.value.map { it.value }.first() }
                .map(::createVulnlogExecution)
        return if (filteredOnReleaseBranch.isEmpty()) {
            null
        } else if (filteredOnReleaseBranch.size > 1) {
            error("Multiple execution actions for the same release branch are currently not supported")
        } else {
            ExecutionDataPerBranch(filteredOnReleaseBranch.first())
        }
    }

private fun createVulnlogExecution(entry: Map.Entry<VulnlogExecution, ReleaseBranchData>): ExecutionPerBranch {
    val key = entry.key
    return when (key) {
        is VulnlogFixExecution -> FixedExecutionPerBranch(fixDate = key.fixDate)
        is VulnlogSuppressPermanentExecution -> SuppressionPermanentExecutionPerBranch()
        is VulnlogSuppressUntilExecution -> SuppressionDateExecutionPerBranch(suppressUntilDate = key.untilDate)
        is VulnlogSuppressUntilNextPublicationExecution -> SuppressionEventExecutionPerBranch()
    }
}

private fun filterInvolvedReleaseVersions(
    releaseBranch: ReleaseBranchData,
    involvedReleaseVersions: BranchToInvolvedVersions?,
    filteredExecution: ExecutionDataPerBranch?,
): InvolvedReleaseVersion? {
    val affected = involvedReleaseVersions?.get(releaseBranch)?.affected
    val upcoming = involvedReleaseVersions?.get(releaseBranch)?.upcoming
    return if (affected == null && upcoming == null) {
        null
    } else if (filteredExecution != null && filteredExecution.execution is SuppressionPermanentExecutionPerBranch) {
        InvolvedReleaseVersionImpl(affected, null)
    } else {
        InvolvedReleaseVersionImpl(affected, upcoming)
    }
}
