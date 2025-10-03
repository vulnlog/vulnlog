package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranchVulnerabilities(
    val releaseBranch: String,
    val vulnerabilities: List<Vulnerability>,
)
