package dev.vulnlog.dslinterpreter.service

import dev.vulnlog.common.InvolvedReleaseVersionImpl
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

data class ReportForBranch(val awareAt: LocalDate, val affected: ReleaseBranchData)

typealias BranchToInvolvedVersions = Map<ReleaseBranchData, InvolvedReleaseVersion>

interface AffectedVersionsService {
    fun findInvolvedVersions(reportForBranches: List<ReportForBranch>): BranchToInvolvedVersions
}

class AffectedVersionsServiceImpl(
    private val branchRepository: BranchRepository,
) : AffectedVersionsService {
    override fun findInvolvedVersions(reportForBranches: List<ReportForBranch>): BranchToInvolvedVersions {
        return reportForBranches
            .associate { (affectedDate, branchData) ->
                val affectedVersion: ReleaseVersionData? =
                    branchRepository.getNextReleaseVersionBefore(branchData, affectedDate)
                        .getOrDefault(null)
                // TODO find the next release after the fixedAt date
                val fixedVersion: ReleaseVersionData? =
                    branchRepository.getNextReleaseVersionAfter(branchData, affectedDate)
                        .getOrDefault(null)
                branchData to InvolvedReleaseVersionImpl(affectedVersion, fixedVersion)
            }
    }
}
