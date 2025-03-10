package dev.vulnlog.cli.commands

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VulnlogData
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch

data class Filtered(
    val releaseBranches: Map<ReleaseBranchData, List<ReleaseVersionData>>,
    val vulnerabilitiesPerBranch: Map<ReleaseBranchData, List<VulnlogData>>,
)

class DslResultFilter(
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
) {
    fun filter(evalResult: VlDslRoot): Filtered {
        val filteredReleaseBranches = filterReleaseBranches(evalResult.branchToReleases)
        val filteredVulnerabilities = filterVulnerabilities(evalResult)

        val releaseBranchesWithVulnerabilities =
            filteredReleaseBranches.filter { (a, _) -> filteredVulnerabilities.keys.contains(a) }

        return Filtered(releaseBranchesWithVulnerabilities, filteredVulnerabilities)
    }

    private fun filterReleaseBranches(
        releaseBranchesToReleaseVersions: Map<ReleaseBranchData, List<ReleaseVersionData>>,
    ): Map<ReleaseBranchData, List<ReleaseVersionData>> {
        return if (filterBranches != null) {
            releaseBranchesToReleaseVersions.filter { filterBranches.contains(it.key.name) }
        } else {
            releaseBranchesToReleaseVersions
        }
    }

    private fun filterVulnerabilities(evalResult: VlDslRoot): Map<ReleaseBranchData, List<VulnlogData>> {
        val vulnerabilities: List<VulnlogData> = evalResult.data
        val splitVulnToBranches = vulnerabilityPerBranch(evalResult.branchToReleases.keys, vulnerabilities)
        return filterAndSplitToSeparateReleaseBranches(splitVulnToBranches)
    }

    private fun filterAndSplitToSeparateReleaseBranches(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnlogData>>,
    ): Map<ReleaseBranchData, List<VulnlogData>> {
        val filteredReleaseBranches = filterOnlySpecifiedReleaseBranches(splitVulnToBranches)
        return filterOnlySpecifiedVulnIds(filteredReleaseBranches)
    }

    private fun filterOnlySpecifiedReleaseBranches(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnlogData>>,
    ): Map<ReleaseBranchData, List<VulnlogData>> {
        return if (filterBranches != null) {
            splitVulnToBranches.filter { filterBranches.contains(it.key.name) }
        } else {
            splitVulnToBranches
        }
    }

    private fun filterOnlySpecifiedVulnIds(splitVulnToBranches: Map<ReleaseBranchData, List<VulnlogData>>) =
        if (filterVulnerabilities != null) {
            splitVulnToBranches
                .map { data ->
                    val vulnerabilityIntersection =
                        data.value.filter { filterVulnerabilities.intersect(it.ids.toSet()).isNotEmpty() }
                    data.key to vulnerabilityIntersection
                }
                .toMap()
                .filter { (_, vulnlogData) -> vulnlogData.isNotEmpty() }
        } else {
            splitVulnToBranches
        }
}
