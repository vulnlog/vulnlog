package dev.vulnlog.cli.service

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.splitter.VulnerabilityDataPerBranch

class StatusService(private val ruleset: AbstractFindResultStatus) {
    fun calculateStatus(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
        splitVulnToBranches.map { (rb, vulnerabilities) ->
            rb to vulnerabilities.map { vulnerability -> vulnerability.copy(status = ruleset.handle(vulnerability)) }
        }.toMap()
}
