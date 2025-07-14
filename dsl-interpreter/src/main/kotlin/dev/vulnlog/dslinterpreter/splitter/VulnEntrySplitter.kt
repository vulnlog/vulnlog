package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.ExecutionPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.InvolvedRelease
import dev.vulnlog.common.InvolvedReleaseVersionImpl
import dev.vulnlog.common.ReleaseVersion
import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.TaskDataPerBranch
import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.BranchVersion
import dev.vulnlog.common.model.ReportBy
import dev.vulnlog.common.model.ReportFor
import dev.vulnlog.common.model.VulnEntry
import dev.vulnlog.common.model.VulnEntryPartialStep1
import dev.vulnlog.common.model.VulnEntryPartialStep2
import dev.vulnlog.common.model.VulnId
import dev.vulnlog.common.model.VulnerabilityData
import dev.vulnlog.common.model.VulnlogReportData
import dev.vulnlog.common.repository.VulnerabilityDataRepository
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.VulnlogAnalysisData
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogFixExecution
import dev.vulnlog.dsl.VulnlogSuppressPermanentExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilNextPublicationExecution
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dslinterpreter.service.BranchToInvolvedVersions
import dev.vulnlog.dslinterpreter.service.StatusService

class VulnEntrySplitter(
    private val vulnerabilityRepository: VulnerabilityDataRepository,
    private val statusService: StatusService,
) {
    @Suppress("LongMethod")
    fun split(): List<VulnEntry> {
        if (vulnerabilityRepository.isEmpty()) return emptyList()

        val result: List<VulnEntry> =
            vulnerabilityRepository.getVulnerabilities().flatMap { groupedVulnerability: VulnerabilityData ->
                val primaryVulnId = VulnId(groupedVulnerability.ids[0])
                val step1: List<VulnEntryPartialStep1> =
                    groupedVulnerability.ids.map { id ->
                        val vulnId = VulnId(id)
                        splitGroupedVulnerability(vulnId, primaryVulnId, groupedVulnerability)
                    }

                val affectedReleaseBranches = groupedVulnerability.reportData?.affected ?: emptyList()
                val step2PerBranch: Map<BranchName, List<VulnEntryPartialStep2>> =
                    affectedReleaseBranches.associate { releaseBranch ->
                        val filteredReport: ReportDataPerBranch? =
                            filterOnReleaseBranch(releaseBranch, groupedVulnerability.reportData)
                        val step2 =
                            filteredReport?.let { report ->
                                val filteredAnalysis: AnalysisDataPerBranch? =
                                    filterOnReleaseBranch(groupedVulnerability.analysisData)
                                val filteredTask: TaskDataPerBranch? =
                                    filterOnReleaseBranch(releaseBranch, groupedVulnerability.taskData)
                                val filteredExecution: ExecutionDataPerBranch? =
                                    filterOnReleaseBranch(releaseBranch, groupedVulnerability.executionData)
                                val filteredInvolvedReleaseVersion: InvolvedReleaseVersion? =
                                    filterInvolvedReleaseVersions(
                                        releaseBranch,
                                        groupedVulnerability.involvedReleaseVersions,
                                        filteredExecution,
                                    )
                                report.reporters.map { reporter ->
                                    val reportBy = ReportBy(reporter.name, report.awareAt)
                                    val involved =
                                        filteredInvolvedReleaseVersion?.let { involved ->
                                            val affected =
                                                involved.affected?.let { affected ->
                                                    ReleaseVersion(
                                                        affected.version,
                                                        affected.releaseDate,
                                                    )
                                                }
                                            val upcoming =
                                                involved.upcoming?.let { upcoming ->
                                                    ReleaseVersion(
                                                        upcoming.version,
                                                        upcoming.releaseDate,
                                                    )
                                                }
                                            InvolvedRelease(affected, upcoming)
                                        }
                                    val reportedFor =
                                        ReportFor(
                                            BranchName(releaseBranch.name),
                                            involved?.affected?.let { BranchVersion(it.version) },
                                        )
                                    VulnEntryPartialStep2(
                                        reportBy,
                                        reportedFor,
                                        filteredAnalysis,
                                        filteredTask,
                                        filteredExecution,
                                        involved,
                                    )
                                }
                            } ?: emptyList()

                        BranchName(releaseBranch.name) to step2
                    }

                val foo: List<VulnEntry> =
                    step1.flatMap { step ->
                        step2PerBranch.entries.flatMap { (_, partial2) ->
                            partial2.map { part ->
                                val status = statusService.calculateStatus(part)
                                VulnEntry(
                                    id = step.id,
                                    primaryVulnId = step.primaryVulnId,
                                    groupIds = step.groupIds,
                                    reportedBy = part.reportedBy,
                                    reportedFor = part.reportedFor,
                                    analysis = part.analysis,
                                    task = part.task,
                                    execution = part.execution,
                                    involved = part.involved,
                                    status = status,
                                )
                            }
                        }
                    }
                foo
            }

        return result
    }

    private fun splitGroupedVulnerability(
        vulnId: VulnId,
        primaryVulnId: VulnId,
        groupedVulnerability: VulnerabilityData,
    ): VulnEntryPartialStep1 {
        val otherVulnIds: List<VulnId> = groupedVulnerability.ids.map(::VulnId).filterNot { it == vulnId }
        return VulnEntryPartialStep1(vulnId, vulnId == primaryVulnId, otherVulnIds)
    }

    private fun filterOnReleaseBranch(
        releaseBranch: ReleaseBranchData,
        reportData: VulnlogReportData?,
    ): ReportDataPerBranch? {
        return reportData?.let { report ->
            val relevant: List<ReleaseBranchData> = report.affected.filter { it == releaseBranch }
            if (relevant.size > 1) {
                error("Multiple vulnerability reports for the same release branch: $releaseBranch")
            } else if (relevant.isEmpty()) {
                null
            } else {
                ReportDataPerBranch(report.reporters, report.awareAt)
            }
        }
    }

    private fun filterOnReleaseBranch(analysisData: VulnlogAnalysisData?): AnalysisDataPerBranch? {
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
        val key: VulnlogExecution = entry.key
        return when (key) {
            is VulnlogFixExecution -> FixedExecutionPerBranch(fixDate = key.fixDate)
            is VulnlogSuppressPermanentExecution -> SuppressionPermanentExecutionPerBranch
            is VulnlogSuppressUntilExecution -> SuppressionDateExecutionPerBranch(suppressUntilDate = key.untilDate)
            is VulnlogSuppressUntilNextPublicationExecution -> {
                SuppressionEventExecutionPerBranch(key.involved.entries.map { it.value.upcoming?.releaseDate }.first())
            }
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
}
