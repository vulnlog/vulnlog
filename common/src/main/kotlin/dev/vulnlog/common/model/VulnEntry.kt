package dev.vulnlog.common.model

import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.InvolvedRelease
import dev.vulnlog.common.TaskDataPerBranch

data class VulnEntry(
    val id: VulnId,
    val primaryVulnId: Boolean,
    val groupIds: List<VulnId>,
    val reportedBy: ReportBy,
    val reportedFor: ReportFor,
    val analysis: AnalysisDataPerBranch?,
    val task: TaskDataPerBranch?,
    val execution: ExecutionDataPerBranch?,
    val involved: InvolvedRelease?,
    val status: VulnStatus,
)

data class VulnEntryPartialStep1(
    val id: VulnId,
    val primaryVulnId: Boolean,
    val groupIds: List<VulnId>,
)

data class VulnEntryPartialStep2(
    val reportedBy: ReportBy,
    val reportedFor: ReportFor,
    val analysis: AnalysisDataPerBranch?,
    val task: TaskDataPerBranch?,
    val execution: ExecutionDataPerBranch?,
    val involved: InvolvedRelease?,
)
