package dev.vulnlog.cli.commands

import dev.vulnlog.dsl2.VlVuln
import dev.vulnlog.dsl2.impl.Vulnlog2FileData
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch2

class SimplePrinter2(
    private val printer: (String) -> Unit,
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
    private val filterVersions: List<String>?,
) {
    fun printNicely(evalResult: Result<Vulnlog2FileData>) {
        evalResult.onFailure {
            error(it)
        }.onSuccess { entry ->
            val splitVulnToBranches = vulnerabilityPerBranch2(entry.releases, entry.vulnerabilities)
            val filtered = filter(splitVulnToBranches)
            printer("---")
            printAllVulnerabilities(filtered)
            printer("---")
            printSummary(filtered)
        }
    }

    private fun filter(splitVulnToBranches: Map<String, List<VlVuln>>): Map<String, List<VlVuln>> {
        val filteredBranches: Map<String, List<VlVuln>> =
            splitVulnToBranches.filter { entry -> filterBranches?.any { branch -> branch == entry.key } ?: true }

        val filtered =
            filteredBranches
                .map { (branchName, vulns) ->
                    if (filterVulnerabilities == null) {
                        branchName to vulns
                    } else {
                        val filteredVulns =
                            vulns.filter { v ->
                                v.ids.map { id -> id.identifier }.intersect(filterVulnerabilities.toSet())
                                    .isNotEmpty()
                            }
                        branchName to filteredVulns
                    }
                }.associate { (branchName, vulns) ->
                    if (filterVersions == null) {
                        branchName to vulns
                    } else {
                        val filteredVulns: List<VlVuln> =
                            vulns.filter { v ->
                                v.reportedFor
                                    .flatMap { rF ->
                                        rF.releaseBranchToReleases.map { rbtr -> rbtr.value.release.version.version }
                                            .toSet()
                                    }
                                    .toSet().intersect(filterVersions.toSet()).isNotEmpty()
                            }
                        branchName to filteredVulns
                    }
                }
        return filtered.filter { it.value.isNotEmpty() }
    }

    private fun printAllVulnerabilities(filtered: Map<String, List<VlVuln>>) {
        filtered
            .forEach { (releaseBranch, vulnerabilities) ->
                printer(releaseBranch)
                vulnerabilities.forEach { vulnerability ->
                    val ids = vulnerability.ids.joinToString(", ") { i -> i.identifier }
                    if (ids.isNotBlank()) printer("  ID: $ids")
                    val reportedFor =
                        vulnerability.reportedFor.joinToString("\n") { r ->
                            "${r.reporter} ${r.at} ${
                                r.getAllForReleaseBranch(
                                    releaseBranch,
                                )
                            }"
                        }
                    if (reportedFor.isNotBlank()) printer("    reported for : $reportedFor")
                    val analysis =
                        vulnerability.rating.joinToString("\n") { "${it.rating} ${it.ratedAt} ${it.reasoning}" }
                    printer("  Analysis: $analysis")
                    val plan = vulnerability.taskPlans.joinToString("\n") { "${it.resolution}" }
                    printer("  Plan: $plan")
                }
            }
    }

    private fun printSummary(filtered: Map<String, List<VlVuln>>) {
        val vulnerabilityCount = filtered.map { it.value }.sumOf { it.size }
        val releaseBranchCount = filtered.map { it.key }.count()
        printer("Found $vulnerabilityCount vulnerabilities in $releaseBranchCount release branches")
    }
}
