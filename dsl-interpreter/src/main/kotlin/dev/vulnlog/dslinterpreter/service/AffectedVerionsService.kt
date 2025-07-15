package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.InvolvedReleaseVersionImpl
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

data class ReportForBranch(val awareAt: LocalDate, val affected: ReleaseBranchData)

interface AffectedVersionsService {
    fun findInvolvedVersions(reportForBranches: ReportForBranch): InvolvedReleaseVersion
}

class AffectedVersionsServiceImpl(
    private val branchRepository: BranchRepository,
) : AffectedVersionsService {
    override fun findInvolvedVersions(reportForBranches: ReportForBranch): InvolvedReleaseVersion {
        val affectedVersion: ReleaseVersionData? =
            branchRepository.getNextReleaseVersionBefore(reportForBranches.affected, reportForBranches.awareAt)
                .getOrDefault(null)
        val fixedVersion: ReleaseVersionData? =
            branchRepository.getNextReleaseVersionAfter(reportForBranches.affected, reportForBranches.awareAt)
                .getOrDefault(null)
        return InvolvedReleaseVersionImpl(affectedVersion, fixedVersion)
    }
}
