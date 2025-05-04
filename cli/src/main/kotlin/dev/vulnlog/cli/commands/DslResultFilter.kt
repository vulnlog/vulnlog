package dev.vulnlog.cli.commands

import Filtered
import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData

class DslResultFilter(
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
) {
    fun filter(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
        releaseBranchesToReleaseVersions: Map<ReleaseBranchData, List<ReleaseVersionData>>,
    ): Filtered {
        val filteredVulnerabilities = filterVulnerabilities(splitVulnToBranches)
        val filteredReleaseBranches = filterReleaseBranches(releaseBranchesToReleaseVersions)
        val releaseBranchesWithVulnerabilities =
            filteredReleaseBranches.filter { (rb, _) -> filteredVulnerabilities.keys.contains(rb) }

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

    private fun filterVulnerabilities(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
        filterAndSplitToSeparateReleaseBranches(splitVulnToBranches)

    private fun filterAndSplitToSeparateReleaseBranches(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
        val filteredReleaseBranches = filterOnlySpecifiedReleaseBranches(splitVulnToBranches)
        return filterOnlySpecifiedVulnIds(filteredReleaseBranches)
    }

    private fun filterOnlySpecifiedReleaseBranches(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ): Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> {
        return if (filterBranches != null) {
            splitVulnToBranches.filter { filterBranches.contains(it.key.name) }
        } else {
            splitVulnToBranches
        }
    }

    private fun filterOnlySpecifiedVulnIds(
        splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
    ) = if (filterVulnerabilities != null) {
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
