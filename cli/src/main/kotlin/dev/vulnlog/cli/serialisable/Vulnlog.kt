package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class Vulnlog(
    val releaseBranches: List<ReleaseBranche>,
)
