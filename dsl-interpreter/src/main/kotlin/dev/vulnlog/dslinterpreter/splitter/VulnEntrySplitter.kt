package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.InvolvedRelease
import dev.vulnlog.common.ReleaseVersion
import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.common.TaskDataPerBranch
import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.BranchVersion
import dev.vulnlog.common.model.ReportBy
import dev.vulnlog.common.model.ReportFor
import dev.vulnlog.common.model.VulnEntry
import dev.vulnlog.common.model.VulnEntryIdData
import dev.vulnlog.common.model.VulnEntryNonIdData
import dev.vulnlog.common.model.VulnId
import dev.vulnlog.common.model.VulnerabilityData
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.service.StatusService

class VulnEntrySplitter(
    private val statusService: StatusService,
    private val reportSplitter: ReportSplitter,
    private val analysisSplitter: AnalysisSplitter,
    private val taskSplitter: TaskSplitter,
    private val executionSplitter: ExecutionSplitter,
    private val involvedReleasesSplitter: InvolvedReleasesSplitter,
) {
    fun split(vulnerabilityData: List<VulnerabilityData>): List<VulnEntry> {
        return vulnerabilityData.flatMap { groupedVulnerability: VulnerabilityData ->
            val primaryVulnId = VulnId(groupedVulnerability.ids.first())
            val splitPerId: List<VulnEntryIdData> = createVulnEntryIdData(groupedVulnerability, primaryVulnId)
            val affectedReleaseBranches = groupedVulnerability.reportData.affected
            val splitPerIdAndBranch: Map<BranchName, List<VulnEntryNonIdData>> =
                affectedReleaseBranches.associate { releaseBranch ->
                    val filteredReport: ReportDataPerBranch? =
                        reportSplitter.filterOnReleaseBranch(releaseBranch, groupedVulnerability.reportData)
                    val nonIdData: List<VulnEntryNonIdData> =
                        createVulnEntryNonIdData(filteredReport, groupedVulnerability, releaseBranch)
                    BranchName(releaseBranch.name) to nonIdData
                }
            createSplitVulnEntries(splitPerId, splitPerIdAndBranch)
        }
    }

    private fun createVulnEntryNonIdData(
        filteredReport: ReportDataPerBranch?,
        groupedVulnerability: VulnerabilityData,
        releaseBranch: ReleaseBranchData,
    ) = filteredReport?.let { report ->
        val filteredAnalysis: AnalysisDataPerBranch? =
            analysisSplitter.filterOnReleaseBranch(groupedVulnerability.analysisData)
        val filteredTask: TaskDataPerBranch? =
            taskSplitter.filterOnReleaseBranch(releaseBranch, groupedVulnerability.taskData)
        val filteredExecution: ExecutionDataPerBranch? =
            executionSplitter.filterOnReleaseBranch(releaseBranch, groupedVulnerability.executionData)
        val filteredInvolvedReleaseVersion: InvolvedReleaseVersion =
            involvedReleasesSplitter.filterInvolvedReleaseVersions(releaseBranch, filteredReport, filteredExecution)
        report.reporters.map { reporter ->
            val reportBy = ReportBy(reporter.name, report.awareAt)
            val involved =
                filteredInvolvedReleaseVersion.let { involved ->
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
                ReportFor(BranchName(releaseBranch.name), involved.affected?.let { BranchVersion(it.version) })
            VulnEntryNonIdData(reportBy, reportedFor, filteredAnalysis, filteredTask, filteredExecution, involved)
        }
    } ?: emptyList()

    private fun createVulnEntryIdData(
        groupedVulnerability: VulnerabilityData,
        primaryVulnId: VulnId,
    ) = groupedVulnerability.ids.map { id ->
        val vulnId = VulnId(id)
        splitGroupedVulnerability(vulnId, primaryVulnId, groupedVulnerability)
    }

    private fun createSplitVulnEntries(
        splitPerId: List<VulnEntryIdData>,
        splitPerIdAndBranch: Map<BranchName, List<VulnEntryNonIdData>>,
    ) = splitPerId.flatMap { vuln ->
        splitPerIdAndBranch.entries.flatMap { (_, vulnPerIdAndBranch) ->
            vulnPerIdAndBranch.map { entry ->
                val status = statusService.calculateStatus(entry)
                VulnEntry(
                    id = vuln.id,
                    primaryVulnId = vuln.primaryVulnId,
                    groupIds = vuln.groupIds,
                    reportedBy = entry.reportedBy,
                    reportedFor = entry.reportedFor,
                    analysis = entry.analysis,
                    task = entry.task,
                    execution = entry.execution,
                    involved = entry.involved,
                    status = status,
                )
            }
        }
    }

    private fun splitGroupedVulnerability(
        vulnId: VulnId,
        primaryVulnId: VulnId,
        groupedVulnerability: VulnerabilityData,
    ): VulnEntryIdData {
        val otherVulnIds: List<VulnId> = groupedVulnerability.ids.map(::VulnId).filterNot { it == vulnId }
        return VulnEntryIdData(vulnId, vulnId == primaryVulnId, otherVulnIds)
    }
}
