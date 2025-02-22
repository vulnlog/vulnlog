package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseBranch(val releaseBranchName: String)
