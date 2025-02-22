package dev.vulnlog.cli.commands

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VulnlogData
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch3

class DslResultFilter(
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
) {
    fun filter(evalResult: VlDslRoot): Map<ReleaseBranchData, List<VulnlogData>> {
        val releaseBranchToReleaseVersions: Map<ReleaseBranchData, List<ReleaseVersionData>> =
            evalResult.branchToReleases
        val vulnerabilities: List<VulnlogData> = evalResult.data
        val splitVulnToBranches = vulnerabilityPerBranch3(releaseBranchToReleaseVersions.keys, vulnerabilities)
        return filterAndSplitToSeparateReleaseBranches(splitVulnToBranches)
    }

    private fun filterAndSplitToSeparateReleaseBranches(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnlogData>>,
    ): Map<ReleaseBranchData, List<VulnlogData>> {
        val filteredReleaseBranches = filterOnlySpecifiedReleaseBranches(splitVulnToBranches)
        return filterOnlySpecifiedVulnIds(filteredReleaseBranches)
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
        } else {
            splitVulnToBranches
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
}
