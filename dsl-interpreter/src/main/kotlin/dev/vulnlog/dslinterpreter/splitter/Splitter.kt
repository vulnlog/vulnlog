@file:Suppress("TooManyFunctions")

package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlOverwriteValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlVulnerabilityValue
import dev.vulnlog.dsl2.TaskActionFixInRelease
import dev.vulnlog.dsl2.VlPlan2
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReleaseBranchDefault
import dev.vulnlog.dsl2.VlReportFor
import dev.vulnlog.dsl2.VlVuln
import dev.vulnlog.dsl3.DefaultReleaseBranchDataImpl
import dev.vulnlog.dsl3.ReleaseBranchData
import dev.vulnlog.dsl3.VulnlogData
import dev.vulnlog.dsl3.VulnlogExecutionData
import dev.vulnlog.dsl3.VulnlogExecutionDataEmpty
import dev.vulnlog.dsl3.VulnlogExecutionDataImpl
import dev.vulnlog.dsl3.VulnlogReportData
import dev.vulnlog.dsl3.VulnlogReportDataEmpty
import dev.vulnlog.dsl3.VulnlogReportDataImpl
import dev.vulnlog.dsl3.VulnlogTaskData
import dev.vulnlog.dsl3.VulnlogTaskDataEmpty
import dev.vulnlog.dsl3.VulnlogTaskDataImpl
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl

/**
 * Utility to group [vulnerabilities] per branch for each branch specified in [branches].
 * Vulnerabilities without reportedFor data are assigned to a default branch.
 */
fun vulnerabilityPerBranch(
    branches: List<VlBranchValue>,
    vulnerabilities: List<VlVulnerabilityData>,
): List<VulnlogPerBranch> {
    return if (vulnerabilities.isEmpty()) {
        emptyList()
    } else if (branches.isEmpty()) {
        listOf(VulnlogPerBranch(vulnerabilities = vulnerabilities))
    } else {
        splitAndGroupByBranch(vulnerabilities, branches)
    }
}

fun vulnerabilityPerBranch2(
    releases: Set<VlReleaseBranch>,
    vulnerabilities: List<VlVuln>,
): Map<String, List<VlVuln>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else if (releases.isEmpty()) {
        mapOf(VlReleaseBranchDefault.name to vulnerabilities)
    } else {
        splitAndGroupByBranch2(vulnerabilities)
    }
}

fun vulnerabilityPerBranch3(
    releases: Set<ReleaseBranchData>,
    vulnerabilities: List<VulnlogData>,
): Map<ReleaseBranchData, List<VulnlogData>> {
    return if (vulnerabilities.isEmpty()) {
        emptyMap()
    } else if (releases.isEmpty()) {
        mapOf(DefaultReleaseBranchDataImpl to vulnerabilities)
    } else {
        splitAndGroupByBranch3(vulnerabilities)
    }
}

/**
 * Split vulnerabilities and group them by branch.
 */
private fun splitAndGroupByBranch(
    vulnerabilities: List<VlVulnerabilityData>,
    branches: List<VlBranchValue>,
): List<VulnlogPerBranch> {
    val splitVulnerabilities = splitIntoMultipleVulnerabilities(vulnerabilities, branches)
    return groupVulnerabilitiesByBranches(splitVulnerabilities)
}

private fun splitAndGroupByBranch2(vulnerabilities: List<VlVuln>): Map<String, List<VlVuln>> {
    val splitVulnerabilities =
        vulnerabilities.map { vulnerability ->
            val releaseBranchesThisVulnerabilityIsReportedFor =
                vulnerability.reportedFor.flatMap { it.releaseBranchToReleases.keys }.toSet()

            releaseBranchesThisVulnerabilityIsReportedFor.associateWith { branchName ->
                val filteredReportFors: List<VlReportFor> =
                    vulnerability.reportedFor
                        .filter { rf -> rf.getAllForReleaseBranch(branchName) != null }
                        .map { rf ->
                            rf.copy(
                                releaseBranchToReleases =
                                    mapOf(branchName to rf.getAllForReleaseBranch(branchName)!!),
                            )
                        }

                val filteredResolutionTasks =
                    vulnerability.resolutionTask
                        .map { r -> r to r.releaseBranches.filter { rb -> rb.name == branchName } }
                        .filter { (_, rb) -> rb.isNotEmpty() }
                        .map { (r, rb) -> r.copy(releaseBranches = rb) }

                val filteredTaskPlan =
                    vulnerability.taskPlans
                        .map { tp ->
                            tp to createPair(tp, branchName)
                        }
                        .filter { (_, rb) -> rb.first.isNotEmpty() }
                        .map { (tp, resolutionAndAction) ->
                            tp.copy(
                                resolution = tp.resolution.copy(releaseBranches = resolutionAndAction.first),
                                taskAction = resolutionAndAction.second,
                            )
                        }

                vulnerability.copy(
                    reportedFor = filteredReportFors,
                    resolutionTask = filteredResolutionTasks,
                    taskPlans = filteredTaskPlan,
                )
            }
        }
            .filter { it.isNotEmpty() }
            .flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
    return splitVulnerabilities
}

private fun splitAndGroupByBranch3(vulnerabilities: List<VulnlogData>): Map<ReleaseBranchData, List<VulnlogData>> {
    val splitVulnerabilities: Map<ReleaseBranchData, List<VulnlogData>> =
        vulnerabilities.map { vulnerability ->
            val affectedReleaseBranches = vulnerability.reportData.affected
            val splitVulnerabilities: Map<ReleaseBranchData, VulnlogData> =
                affectedReleaseBranches.associateWith { releaseBranch ->
                    val filteredReport = filterOnReleaseBranch(releaseBranch, vulnerability.reportData)
                    val filteredTask = filterOnReleaseBranch(releaseBranch, vulnerability.taskData)
                    val filteredExecution = filterOnReleaseBranch(releaseBranch, vulnerability.executionData)
                    vulnerability.copy(
                        reportData = filteredReport,
                        taskData = filteredTask,
                        executionData = filteredExecution,
                    )
                }
            splitVulnerabilities
        }
            .filter { it.isNotEmpty() }
            .flatMap { it.entries }
            .groupBy({ it.key }, { it.value })
    return splitVulnerabilities
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    reportData: VulnlogReportData,
): VulnlogReportData {
    return when (reportData) {
        is VulnlogReportDataEmpty -> reportData
        is VulnlogReportDataImpl -> reportData.copy(affected = reportData.affected.filter { it == releaseBranch })
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    taskData: VulnlogTaskData,
): VulnlogTaskData {
    return when (taskData) {
        is VulnlogTaskDataEmpty -> taskData
        is VulnlogTaskDataImpl -> {
            val filteredOnReleaseBranch =
                taskData.taskOnReleaseBranch.entries
                    .associate { it.key to it.value.filter { rb -> rb == releaseBranch } }
                    .filter { it.value.isNotEmpty() }
            taskData.copy(taskOnReleaseBranch = filteredOnReleaseBranch)
        }
    }
}

fun filterOnReleaseBranch(
    releaseBranch: ReleaseBranchData,
    executionData: VulnlogExecutionData,
): VulnlogExecutionData {
    return when (executionData) {
        is VulnlogExecutionDataEmpty -> executionData
        is VulnlogExecutionDataImpl -> {
            val filteredOnReleaseBranch = executionData.tasks.filter { it.releases.contains(releaseBranch) }
            executionData.copy(tasks = filteredOnReleaseBranch)
        }
    }
}

private fun createPair(
    tp: VlPlan2,
    branchName: String,
) = Pair(
    tp.resolution.releaseBranches.filter { rb -> rb.name == branchName },
    tp.taskAction.filter { ta ->
        when (ta) {
            is TaskActionFixInRelease -> ta.releaseBranchName == branchName
            else -> true
        }
    },
)

private fun splitIntoMultipleVulnerabilities(
    vulnerabilities: List<VlVulnerabilityData>,
    branches: List<VlBranchValue>,
): List<VulnlogPerBranch> {
    return vulnerabilities
        .flatMap { data ->
            // TODO handle two releases within the same branch as one (the latest should be considered) do not
            //  generate two vulnerabilities.
            val splitVulnerabilities: List<VlVulnerabilityData> =
                if (data.vulnerability.reportFor.isNotEmpty()) {
                    createCopyOfVulnerabilityData(data)
                } else {
                    listOf(data)
                }

            // handle overwrites
            val overwrittenSplitVulnerabilities =
                splitVulnerabilities.map { vulnData ->
                    val overwrites = vulnData.vulnerability.overwrites

                    // filter branch relevant overwrites
                    val relevantReleases: Set<VlOverwriteValue> =
                        overwrites.filter { overwrite ->
                            overwrite.reportedFor.map(VlReportForValue::release)
                                .toSet()
                                .intersect(vulnData.vulnerability.reportFor.map(VlReportForValue::release).toSet())
                                .isNotEmpty()
                        }.toSet()

                    val effectiveOverwrites: Set<VlOverwriteValue> =
                        relevantReleases.filter { overwrite ->
                            overwrite.reportedFor.intersect(vulnData.vulnerability.reportFor).isNotEmpty()
                        }.toSet()

                    createOverwrittenCopy(vulnData, effectiveOverwrites, data)
                }

            val vulnerabilitiesPerBranch: List<VulnlogPerBranch> =
                mapToVulnlogPerBranch(overwrittenSplitVulnerabilities, branches)
            filterFixInVersions(vulnerabilitiesPerBranch)
        }.toList()
}

private fun createOverwrittenCopy(
    vulnData: VlVulnerabilityData,
    effectiveOverwrites: Set<VlOverwriteValue>,
    data: VlVulnerabilityData,
): VlVulnerabilityData {
    val reportBy =
        (effectiveOverwrites.firstOrNull { it.reportBy.isNotEmpty() }?.reportBy ?: vulnData.vulnerability.reportBy)
    val rating = effectiveOverwrites.firstOrNull { it.rating != null }?.rating ?: vulnData.vulnerability.rating
    val fixAction = effectiveOverwrites.firstOrNull { it.toFix != null }?.toFix ?: vulnData.vulnerability.fixAction
    val fixIn = effectiveOverwrites.firstOrNull { it.fixIn.isNotEmpty() }?.fixIn ?: vulnData.vulnerability.fixIn
    val overwritten: VlVulnerabilityValue =
        (vulnData.vulnerability as VlVulnerabilityValueImpl).copy(
            reportBy = reportBy,
            rating = rating,
            fixAction = fixAction,
            fixIn = fixIn,
            overwrites = emptySet(),
        )
    return data.copy(vulnerability = overwritten)
}

private fun createCopyOfVulnerabilityData(data: VlVulnerabilityData) =
    data.vulnerability.reportFor
        .map { reportedFor -> (data.vulnerability as VlVulnerabilityValueImpl).copy(reportFor = setOf(reportedFor)) }
        .map { vulnerability -> data.copy(vulnerability = vulnerability) }

private fun mapToVulnlogPerBranch(
    vulnerabilities: List<VlVulnerabilityData>,
    branches: List<VlBranchValue>,
): List<VulnlogPerBranch> {
    return vulnerabilities.groupBy { data ->
        val reportedForReleases: Set<VlReleaseValue> = data.vulnerability.reportFor.map { it.release }.toSet()
        branches.firstOrNull { branch -> branch.releases.toSet().intersect(reportedForReleases).isNotEmpty() }
    }.map { VulnlogPerBranch(it.key ?: DefaultBranch, it.value) }
}

/**
 * Remove fixIn versions not related to the branch
 */
private fun filterFixInVersions(vulnPerBranches: List<VulnlogPerBranch>): List<VulnlogPerBranch> {
    return vulnPerBranches
        .map { vulnPerBranch ->
            val branchReleases: List<VlReleaseValue> = vulnPerBranch.branch.releases
            val filteredVulnerabilityData =
                vulnPerBranch.vulnerabilities.map { vulnerability ->
                    createCopyOfFixInFilteredVulnerabilityData(branchReleases, vulnerability)
                }
            VulnlogPerBranch(vulnPerBranch.branch, filteredVulnerabilityData)
        }.toList()
}

private fun createCopyOfFixInFilteredVulnerabilityData(
    branchReleases: List<VlReleaseValue>,
    data: VlVulnerabilityData,
): VlVulnerabilityData {
    val fixIn: Set<VlReleaseValue> =
        if (branchReleases.isNotEmpty()) {
            branchReleases.intersect(data.vulnerability.fixIn)
        } else {
            data.vulnerability.fixIn
        }
    val vulnerability = (data.vulnerability as VlVulnerabilityValueImpl).copy(fixIn = fixIn)
    return data.copy(vulnerability = vulnerability)
}

private fun groupVulnerabilitiesByBranches(vulnerabilitiesPerBranch: List<VulnlogPerBranch>) =
    vulnerabilitiesPerBranch.groupBy { it.branch }
        .map { it.key to it.value.flatMap { vuln -> vuln.vulnerabilities }.toList() }
        .map { VulnlogPerBranch(it.first, it.second) }
