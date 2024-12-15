package dev.vulnlog.cli.commands

import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VulnlogFileData
import dev.vulnlog.dslinterpreter.splitter.VulnlogPerBranch
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch

class SimplePrinter(
    private val printer: (String) -> Unit,
    private val filterVulnerabilities: List<String>?,
    private val filterBranches: List<String>?,
    private val filterVersions: List<String>?,
) {
    fun printNicely(evalResult: Result<VulnlogFileData>) {
        evalResult.onFailure {
            error(it)
        }.onSuccess {
            val splitVulnToBranches = vulnerabilityPerBranch(it.branches, it.vulnerabilities)
            val filtered = filter(splitVulnToBranches, filterVulnerabilities, filterVersions)
            printer("---")
            printAllVulnerabilities(filtered)
            printer("---")
            printSummary(filtered)
        }
    }

    private fun filter(
        splitVulnToBranches: List<VulnlogPerBranch>,
        filterVulnerabilities: List<String>?,
        filterVersions: List<String>?,
    ): List<VulnlogPerBranch> {
        val filteredBranches =
            splitVulnToBranches.filter { entry ->
                filterBranches?.any { branch -> branch == entry.branch.name } ?: true
            }

        val filtered =
            filteredBranches
                .map { entry ->
                    if (filterVulnerabilities == null) {
                        entry
                    } else {
                        val filteredVulns: List<VlVulnerabilityData> =
                            entry.vulnerabilities.filter { v ->
                                v.vulnerabilityIds.map { id -> id.id }.intersect(filterVulnerabilities.toSet())
                                    .isNotEmpty()
                            }
                        VulnlogPerBranch(entry.branch, filteredVulns)
                    }
                }.map { entry ->
                    if (filterVersions == null) {
                        entry
                    } else {
                        val filteredVulns: List<VlVulnerabilityData> =
                            entry.vulnerabilities.filter { v ->
                                v.vulnerability.reportFor.map { rF -> rF.release.version }.toSet()
                                    .intersect(filterVersions.toSet()).isNotEmpty()
                            }
                        VulnlogPerBranch(entry.branch, filteredVulns)
                    }
                }
        return filtered
    }

    private fun printAllVulnerabilities(filtered: List<VulnlogPerBranch>) {
        filtered
            .filter { entry -> entry.vulnerabilities.isNotEmpty() }
            .forEach { entry ->
                printer(entry.branch.name)
                entry.vulnerabilities.forEach { vulnerability ->
                    val ids = vulnerability.vulnerabilityIds.joinToString(", ") { i -> i.id }
                    if (ids.isNotBlank()) printer("  ID: $ids")
                    val reportedFor =
                        vulnerability.vulnerability.reportFor.joinToString(", ") { r -> r.release.version }
                    if (reportedFor.isNotBlank()) printer("    reported for : $reportedFor")
                    val fixIn =
                        vulnerability.vulnerability.fixIn.joinToString(", ", transform = VlReleaseValue::version)
                    if (fixIn.isNotBlank()) printer("    fix in       : $fixIn")
                    val analysis = vulnerability.vulnerability.rating
                    val analysisSummary = "${analysis?.rating} (${analysis?.dateOfAnalysing}) -- ${
                        analysis?.reasoning?.replace(
                            Regex("\\s+"),
                            " ",
                        )?.replace("\n", "")
                    }"
                    if (analysisSummary.isNotBlank()) printer("    analysis     : $analysisSummary")
                    val toFixAction = vulnerability.vulnerability.fixAction?.action ?: ""
                    if (toFixAction.isNotBlank()) printer("    fix action   : $toFixAction")
                }
            }
    }

    private fun printSummary(filtered: List<VulnlogPerBranch>) {
        val vulnerabilityCount = filtered.sumOf { e -> e.vulnerabilities.size }
        val releaseBranchCount = filtered.count { e -> e.vulnerabilities.isNotEmpty() }
        printer("Found $vulnerabilityCount vulnerabilities in $releaseBranchCount release branches")
    }
}
