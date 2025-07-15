package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.InvolvedReleaseVersionImpl
import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.service.AffectedVersionsService
import dev.vulnlog.dslinterpreter.service.ReportForBranch
import java.time.LocalDate

class InvolvedReleasesSplitter(private val affectedVersionService: AffectedVersionsService) {
    fun filterInvolvedReleaseVersions(
        releaseBranch: ReleaseBranchData,
        filteredReport: ReportDataPerBranch,
        filteredExecution: ExecutionDataPerBranch?,
    ): InvolvedReleaseVersion {
        val reportedAtDate: LocalDate = filteredReport.awareAt
        val fixedAtDate: LocalDate? =
            filteredExecution?.let { execution ->
                if (execution.execution is FixedExecutionPerBranch) {
                    (execution.execution as FixedExecutionPerBranch).fixDate
                } else {
                    null
                }
            }

        val affected: InvolvedReleaseVersion =
            affectedVersionService.findInvolvedVersions(ReportForBranch(reportedAtDate, releaseBranch))
        val fixed: InvolvedReleaseVersion? =
            fixedAtDate?.let { fixedAt ->
                affectedVersionService.findInvolvedVersions(ReportForBranch(fixedAt, releaseBranch))
            }
        return if (fixed != null) {
            InvolvedReleaseVersionImpl(affected.affected, fixed.upcoming)
        } else {
            InvolvedReleaseVersionImpl(affected.affected, affected.upcoming)
        }
    }
}
