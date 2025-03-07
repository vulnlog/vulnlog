package dev.vulnlog.dsl

import dev.vulnlog.dsl.VlRelease.Companion.createRelease

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
