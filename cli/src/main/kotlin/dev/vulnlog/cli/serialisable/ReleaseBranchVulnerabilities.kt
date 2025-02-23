package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranchVulnerabilities(
    val releaseBranch: String,
    val vulnerabilities: List<Vulnerability>,
)
