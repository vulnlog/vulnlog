package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranches(
    val releaseBranchesAndVersions: Map<ReleaseBranch, List<ReleaseVersion>>,
)
