package dev.vulnlog.cli.service

import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData

class StatusService(private val ruleset: AbstractFindResultStatus) {
    fun calculateStatus(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
        splitVulnToBranches.map { (rb, vulnerabilities) ->
            rb to vulnerabilities.map { vulnerability -> vulnerability.copy(status = ruleset.handle(vulnerability)) }
        }.toMap()
}
