package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlReportForValue
import dev.vulnlog.dsl.VlVulnerabilityValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlReportForValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityValueImpl

/**
 * Utility to group [vulnerabilities] per branch for each branch specified in [branches].
 * Vulnerabilities without reportedFor data are assigned to a default branch.
 */
fun vulnerabilityPerBranch(
    branches: List<VlBranchValue>,
    vulnerabilities: List<VlVulnerabilityData>,
): List<VulnlogPerBranch> {
    val vulnlogPerBranches =
        branches.map { branch -> VulnlogPerBranch(branch, collectVulnerabilitiesForBranch(branch, vulnerabilities)) }

    val vulnerabilitiesWithoutBranch = vulnerabilities.filter { it.vulnerability.reportFor.isEmpty() }.toList()
    return if (vulnerabilitiesWithoutBranch.isNotEmpty()) {
        vulnlogPerBranches + VulnlogPerBranch(null, vulnerabilitiesWithoutBranch)
    } else {
        vulnlogPerBranches
    }
}

private fun collectVulnerabilitiesForBranch(
    branch: VlBranchValue,
    vulnerabilities: List<VlVulnerabilityData>,
): List<VlVulnerabilityData> {
    return vulnerabilities
        .map { vulnerability -> vulnerability to reportedForOnlyThisBranch(vulnerability, branch) }
        .filter { it.second.isNotEmpty() }
        .map { (vulnerability, reportedForSet) -> copyOfVulnerabilityData(vulnerability, reportedForSet) }
        .toList()
}

private fun copyOfVulnerabilityData(
    src: VlVulnerabilityData,
    reportedForSet: Set<VlReportForValue>,
): VlVulnerabilityData =
    src.copy(vulnerability = copyVulnerability(src.vulnerability as VlVulnerabilityValueImpl, reportedForSet))

private fun copyVulnerability(
    src: VlVulnerabilityValueImpl,
    reportedForSet: Set<VlReportForValue>,
): VlVulnerabilityValue = src.copy(reportFor = reportedForSet)

private fun reportedForOnlyThisBranch(
    vulnerability: VlVulnerabilityData,
    branch: VlBranchValue,
): Set<VlReportForValue> {
    val reportedFor = vulnerability.vulnerability.reportFor
    val releases = reportedFor.map { it.versions }.toSet()
    return reportedFor
        .map { it.variant to branch.releases.intersect(releases) }
        .flatMap { (variant, releases) -> releases.map { VlReportForValueImpl(variant, it) }.toSet() }
        .toSet()
}
