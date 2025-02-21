package dev.vulnlog.cli.commands

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VulnlogData
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch3

class SimplePrinter(
    private val printer: (String) -> Unit,
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
) {
    fun printNicely(evalResult: Result<VlDslRoot>) {
        evalResult.onFailure {
            error(it)
        }.onSuccess { entry ->
            val releaseBranchToReleaseVersions: Map<ReleaseBranchData, List<ReleaseVersionData>> =
                entry.branchToReleases
            val vulnerabilities: List<VulnlogData> = entry.data
            val splitVulnToBranches = vulnerabilityPerBranch3(releaseBranchToReleaseVersions.keys, vulnerabilities)
            val filtered = filter(splitVulnToBranches)
            printer("---")
            printAllVulnerabilities(filtered)
            printer("---")
            printSummary(filtered)
        }
    }

    private fun filter(
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

    private fun printAllVulnerabilities(filtered: Map<ReleaseBranchData, List<VulnlogData>>) {
        filtered
            .forEach { (releaseBranch, vulnerabilities) ->
                printer(releaseBranch.name)
                vulnerabilities.forEach { vulnerability ->
                    val ids = vulnerability.ids
                    if (ids.isNotEmpty()) printer("  ID: ${ids.joinToString(", ")}")
                    val affected = vulnerability.reportData.affected.joinToString(", ") { it.name }
                    if (affected.isNotEmpty()) printer("    affected: $affected")
                    val analysisData = vulnerability.analysisData
                    val analysis = "${analysisData.verdict} ${analysisData.analysedAt} ${analysisData.reasoning}"
                    printer("    analysis: $analysis")
                    val taskData = vulnerability.taskData
                    val plan =
                        taskData.taskOnReleaseBranch.entries.joinToString("\n") {
                            "        ${it.key} on ${
                                it.value.joinToString(
                                    ", ",
                                ) { br -> br.name }
                            }"
                        }
                    printer("    plan:")
                    printer(plan)
                }
            }
    }

    private fun printSummary(filtered: Map<ReleaseBranchData, List<VulnlogData>>) {
        val vulnerabilityCount = filtered.map { it.value }.sumOf { it.size }
        val releaseBranchCount = filtered.map { it.key }.count()
        printer("Found $vulnerabilityCount vulnerabilities in $releaseBranchCount release branches")
    }
}
