package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.VerdictSpecification
import dev.vulnlog.dsl.VlReporter
import dev.vulnlog.dsl.VulnerabilityData
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
import dev.vulnlog.dslinterpreter.service.BranchToInvolvedVersions
import java.time.LocalDate

data class VulnerabilityDataPerBranch(
    val branch: ReleaseBranchData,
    val ids: List<String>,
    val reportData: ReportDataPerBranch,
    val analysisData: AnalysisDataPerBranch? = null,
    val taskData: TaskDataPerBranch? = null,
    val executionData: ExecutionDataPerBranch? = null,
    val involvedReleaseVersions: InvolvedReleaseVersion? = null,
)

data class ReportDataPerBranch(
    val reporter: VlReporter,
    val awareAt: LocalDate,
)

data class AnalysisDataPerBranch(
    val analysedAt: LocalDate,
    val verdict: VerdictSpecification,
    val reasoning: String,
)

data class TaskDataPerBranch(
    val taskAction: TaskAction,
)

data class ExecutionDataPerBranch(
    val execution: ExecutionPerBranch,
)

sealed interface ExecutionPerBranch {
    val action: String
}

data class FixedExecutionPerBranch(
    override val action: String = "fix",
    val fixDate: LocalDate,
) : ExecutionPerBranch

data class SuppressionPermanentExecutionPerBranch(
    override val action: String = "suppress",
) : ExecutionPerBranch

data class SuppressionDateExecutionPerBranch(
    override val action: String = "suppress",
    val suppressUntilDate: LocalDate,
) : ExecutionPerBranch

data class SuppressionEventExecutionPerBranch(
    override val action: String = "suppress",
) : ExecutionPerBranch

/**
 * Splits a vulnerability report into distinct reports per affected release branch.
 */
fun vulnerabilityPerBranch(
    vulnerabilities: List<VulnerabilityData>,
): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else {
        splitAndGroupByBranch(vulnerabilities)
    }
}

private fun splitAndGroupByBranch(
    vulnerabilities: List<VulnerabilityData>,
): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
    val splitVulnerabilities: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
        vulnerabilities.asSequence().map { vulnerability ->
            val affectedReleaseBranches = vulnerability.reportData?.affected ?: emptyList()
            val splitVulnerabilities: Map<ReleaseBranchData, VulnerabilityDataPerBranch?> =
                affectedReleaseBranches.associateWith { releaseBranch ->
                    val filteredReport = filterOnReleaseBranch(releaseBranch, vulnerability.reportData)
                    filteredReport?.let { report ->
                        val filteredAnalysis = filterOnReleaseBranch(vulnerability.analysisData)
                        val filteredTask = filterOnReleaseBranch(releaseBranch, vulnerability.taskData)
                        val filteredExecution = filterOnReleaseBranch(releaseBranch, vulnerability.executionData)
                        val filteredInvolvedReleaseVersion =
                            filterInvolvedReleaseVersions(releaseBranch, vulnerability.involvedReleaseVersions)
                        VulnerabilityDataPerBranch(
                            releaseBranch,
                            vulnerability.ids,
                            report,
                            filteredAnalysis,
                            filteredTask,
                            filteredExecution,
                            filteredInvolvedReleaseVersion,
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
            ReportDataPerBranch(reportData.reporter, reportData.awareAt)
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
): InvolvedReleaseVersion? {
    val affected = involvedReleaseVersions?.get(releaseBranch)?.affected
    val upcoming = involvedReleaseVersions?.get(releaseBranch)?.upcoming
    return if (affected == null && upcoming == null) {
        null
    } else {
        return InvolvedReleaseVersionImpl(affected, upcoming)
    }
}
