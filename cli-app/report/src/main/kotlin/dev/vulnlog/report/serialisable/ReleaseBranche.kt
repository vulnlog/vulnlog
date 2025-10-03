package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranche(
    val releaseBranch: String,
    val releaseVersions: List<ReleaseVersion>,
)
