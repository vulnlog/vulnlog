package dev.vulnlog.suppression

import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporterImpl

class SuppressionGenerator(private val suppressionConfig: SuppressionConfig) {
    fun mapVulnsPerBranchAndReporter(): Set<VulnPerBranchAndRecord> {
        val splitVulns =
            splitVulnerabilitiesPerBranchAndReporter(suppressionConfig.releaseBranches?.vulnerabilitiesPerBranch)

        return splitVulns.flatMap { (branch, reportertoVulns) ->
            reportertoVulns.flatMap { (reporter, vulns) ->
                vulns.map { vuln -> VulnPerBranchAndRecord(branch, reporter, vuln) }
            }
        }.toSet()
    }

    private fun splitVulnerabilitiesPerBranchAndReporter(
        releaseBranchToVulnerabilities: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>?,
    ): Set<Pair<ReleaseBranchData, Map<VlReporterImpl, List<VulnerabilityDataPerBranch>>>> {
        return releaseBranchToVulnerabilities?.map { (branch, vulns) ->
            branch to vulns.map { vuln -> replicateVulnPerIdAndGroupByReporter(vuln) }.reduce { acc, map -> acc + map }
        }?.toSet() ?: emptySet()
    }

    private fun replicateVulnPerIdAndGroupByReporter(
        vuln: VulnerabilityDataPerBranch,
    ): Map<VlReporterImpl, List<VulnerabilityDataPerBranch>> {
        val vulnPerId = vuln.ids.map { id -> vuln.copy(ids = listOf(id)) }
        return vuln.reportData.reporters
            .filterIsInstance<VlReporterImpl>()
            .filter { it.config != null }
            .map { reporter -> reporter to vulnPerId }
            .toMap()
    }
}
