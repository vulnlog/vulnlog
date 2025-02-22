package dev.vulnlog.cli.commands

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VulnlogData

class SimplePrinter(private val printer: (String) -> Unit) {
    fun printNicely(data: Map<ReleaseBranchData, List<VulnlogData>>) {
        printer("---")
        printAllVulnerabilities(data)
        printer("---")
        printSummary(data)
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
