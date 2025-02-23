package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranche(
    val releaseBranch: String,
    val releaseVersions: List<ReleaseVersion>,
)
