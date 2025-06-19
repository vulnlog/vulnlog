package dev.vulnlog.cli.service

import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.VulnEntry
import dev.vulnlog.common.model.VulnId
import dev.vulnlog.dsl.ReleaseBranchData

class VulnEntryFilterService(
    private val filterVulnerabilities: List<String>,
    private val filterBranches: List<String>,
) {
    fun filterVulnEntries(vulnEntries: List<VulnEntry>): List<VulnEntry> {
        val filteredBranches = filterBranches(vulnEntries)
        val filteredEntries = filterVulnIds(filteredBranches)
        return filteredEntries
    }

    fun filterReleaseBranches(releaseBranches: Set<ReleaseBranchData>): Set<BranchName> {
        return filterBranches(releaseBranches)
    }

    private fun filterBranches(vulnEntry: List<VulnEntry>): List<VulnEntry> {
        return if (filterBranches.isEmpty()) {
            vulnEntry
        } else {
            val filterConditions: Set<BranchName> = filterBranches.map(::BranchName).toSet()
            vulnEntry.filter { entry -> filterConditions.contains(entry.reportedFor.branchName) }
        }
    }

    private fun filterVulnIds(vulnEntry: List<VulnEntry>): List<VulnEntry> {
        return if (filterVulnerabilities.isEmpty()) {
            vulnEntry
        } else {
            val filterConditions: Set<VulnId> = filterVulnerabilities.map(::VulnId).toSet()
            vulnEntry.filter { entry -> filterConditions.contains(entry.id) }
        }
    }

    private fun filterBranches(releaseBranches: Set<ReleaseBranchData>): Set<BranchName> {
        val branches: Set<BranchName> = releaseBranches.map { BranchName(it.name) }.toSet()
        return if (filterBranches.isEmpty()) {
            branches
        } else {
            val filterConditions = filterBranches.map(::BranchName).toSet()
            branches.filter(filterConditions::contains).toSet()
        }
    }
}
