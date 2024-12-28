package dev.vulnlog.dsl3

import dev.vulnlog.dsl3.VlRelease.Companion.createRelease

class VlBranchContextImpl(releaseBranchName: String) : VlBranchContext {
    val releaseBranch = ReleaseBranch.create(releaseBranchName)
    val releaseVersions = mutableListOf<VlRelease>()

    override fun release(
        version: String,
        publicationDate: String?,
    ) {
        releaseVersions += createRelease(version, publicationDate)
    }
}
