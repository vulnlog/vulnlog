package dev.vulnlog.suppression

import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporterImpl

class SuppressionGenerator(private val suppressionConfig: SuppressionConfig) {
    fun mapVulnsPerBranchAndReporter(): Set<VulnsPerBranchAndRecord> {
        val splitVulns =
            splitVulnerabilitiesPerBranchAndReporter(suppressionConfig.releaseBranches.vulnerabilitiesPerBranch)

        return splitVulns.flatMap { (branch, reporterToVulns) ->
            reporterToVulns
                .map { (reporter, vulns) ->
                    val filteredById = vulns.filter { it.ids[0].startsWith(reporter.config!!.idMatcher) }
                    VulnsPerBranchAndRecord(branch, reporter, filteredById.toSet())
                }
        }.toSet()
    }

    private fun splitVulnerabilitiesPerBranchAndReporter(
        releaseBranchToVulnerabilities: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>?,
    ): Set<Pair<ReleaseBranchData, Map<VlReporterImpl, List<VulnerabilityDataPerBranch>>>> {
        return releaseBranchToVulnerabilities?.map { (branch, vulns) ->
            val reporterToVulns: Map<VlReporterImpl, List<VulnerabilityDataPerBranch>> =
                vulns.map { vuln -> replicateVulnPerIdAndGroupByReporter(vuln) }
                    .filter { it.isNotEmpty() }
                    .flatMap { it.entries }
                    .groupBy({ it.key }, { it.value })
                    .mapValues { (_, lists) -> lists.flatten() }
            branch to reporterToVulns
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
