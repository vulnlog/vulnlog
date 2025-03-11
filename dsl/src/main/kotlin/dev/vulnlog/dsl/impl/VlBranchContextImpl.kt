package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.VlBranchContext
import dev.vulnlog.dsl.VlRelease
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
