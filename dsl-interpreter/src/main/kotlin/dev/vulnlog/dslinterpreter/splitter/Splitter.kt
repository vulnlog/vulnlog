package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlVulnerabilityValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportForValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl

private data class GenericFilter(val reportedFor: Set<VlReportForValue>, val fixIn: Set<VlReleaseValue>) {
    fun isVulnerabilityRelevantForThisReleaseBranch(): Boolean = reportedFor.isNotEmpty()
}

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
        listOf(VulnlogPerBranch(null, vulnerabilities))
    } else {
        val vulnlogPerBranches: List<VulnlogPerBranch> =
            branches.map { branch ->
                VulnlogPerBranch(
                    branch,
                    collectVulnerabilitiesForBranch(branch, vulnerabilities),
                )
            }

        val vulnerabilitiesWithoutBranch = vulnerabilities.filter { it.vulnerability.reportFor.isEmpty() }.toList()
        val result =
            if (vulnerabilitiesWithoutBranch.isNotEmpty()) {
                vulnlogPerBranches + VulnlogPerBranch(null, vulnerabilitiesWithoutBranch)
            } else {
                vulnlogPerBranches
            }

        result
    }
}

private fun collectVulnerabilitiesForBranch(
    branch: VlBranchValue,
    vulnerabilities: List<VlVulnerabilityData>,
): List<VlVulnerabilityData> {
    return vulnerabilities
        .map { it to genericFilter(it.vulnerability, branch.releases) }
        .filter { (_, filter) -> filter.isVulnerabilityRelevantForThisReleaseBranch() }
        .map { (data, filter) -> data to genericCopier(data.vulnerability, filter) }
        .map { (data, vulnerability) -> data.copy(vulnerability = vulnerability) }
        .toList()
}

private fun genericFilter(
    vulnerability: VlVulnerabilityValue,
    releases: List<VlReleaseValue>,
): GenericFilter {
    val filteredReportedFor = filterReportedFor(vulnerability.reportFor, releases)
    val filteredFixIn = filterFixIn(vulnerability.fixIn, releases)

    return GenericFilter(filteredReportedFor, filteredFixIn)
}

private fun filterReportedFor(
    reportedFor: Set<VlReportForValue>,
    releasesInReleaseBranch: List<VlReleaseValue>,
): Set<VlReportForValue> {
    val releases = reportedFor.map(VlReportForValue::release).toSet()
    return reportedFor
        .map { it.variant to releasesInReleaseBranch.intersect(releases) }
        .flatMap { (variant, releases) -> releases.map { VlReportForValueImpl(variant, it) }.toSet() }
        .toSet()
}

private fun filterFixIn(
    fixIn: Set<VlReleaseValue>,
    releasesInReleaseBranch: List<VlReleaseValue>,
): Set<VlReleaseValue> {
    return fixIn.intersect(releasesInReleaseBranch.toSet()).toSet()
}

private fun genericCopier(
    vulnerability: VlVulnerabilityValue,
    filter: GenericFilter,
): VlVulnerabilityValue {
    var newVulnerability: VlVulnerabilityValue? = null
    if (filter.reportedFor.isNotEmpty()) {
        newVulnerability = (vulnerability as VlVulnerabilityValueImpl).copy(reportFor = filter.reportedFor)
    }
    if (filter.fixIn.isNotEmpty()) {
        newVulnerability = (newVulnerability as VlVulnerabilityValueImpl).copy(fixIn = filter.fixIn)
    }
    return newVulnerability ?: vulnerability
}
