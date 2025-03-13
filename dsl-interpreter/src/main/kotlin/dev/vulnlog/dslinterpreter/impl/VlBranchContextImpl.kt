package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchProvider
import dev.vulnlog.dsl.VlBranchContext
import dev.vulnlog.dsl.VlRelease
import dev.vulnlog.dsl.VlRelease.Companion.createRelease

class VlBranchContextImpl(releaseBranchName: String) : VlBranchContext {
    val releaseBranch = ReleaseBranchProvider.create(releaseBranchName)
    val releaseVersions = mutableListOf<VlRelease>()

    override fun release(
        version: String,
        publicationDate: String?,
    ) {
        releaseVersions += createRelease(version, publicationDate)
    }
}
