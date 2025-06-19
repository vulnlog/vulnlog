package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class Vulnlog(
    val releaseBranches: List<ReleaseBranche>,
    val vulnerabilities: List<Vulnerability>,
)
