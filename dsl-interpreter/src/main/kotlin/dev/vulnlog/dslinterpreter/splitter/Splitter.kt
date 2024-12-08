package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlOverwriteValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlVulnerabilityValue
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
