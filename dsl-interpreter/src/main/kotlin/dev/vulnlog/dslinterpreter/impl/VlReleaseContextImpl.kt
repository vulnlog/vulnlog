package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseBranchProvider
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlBranchContext
import dev.vulnlog.dsl.VlRelease
import dev.vulnlog.dsl.VlRelease.Companion.createRelease
import dev.vulnlog.dsl.VlReleaseContext

class VlReleaseContextImpl : VlReleaseContext {
    private val defaultReleaseBranch = ReleaseBranchProvider.create("default release branch")
    private val defaultReleasesVersion = mutableListOf<VlRelease>()
    private val branchToReleases = mutableMapOf(defaultReleaseBranch to defaultReleasesVersion)

    override fun release(
        version: String,
        publicationDate: String?,
    ) {
        branchToReleases[defaultReleaseBranch]!! += createRelease(version, publicationDate)
    }

    override fun branch(
        name: String,
        block: (VlBranchContext.() -> Unit)?,
    ) = with(VlBranchContextImpl(name)) {
        block?.let { it(this) }
        branchToReleases[releaseBranch] = releaseVersions
    }

    fun getBranchToReleases(): Map<ReleaseBranchData, List<ReleaseVersionData>> {
        return branchToReleases.entries
            .associate { (releaseBranchData, releaseVersionData) ->
                val releaseBranch =
                    if (releaseBranchData == defaultReleaseBranch) {
                        DefaultReleaseBranchDataImpl
                    } else {
                        ReleaseBranchDataImpl(releaseBranchData.name)
                    }
                val releaseVersions =
                    releaseVersionData.map { release -> ReleaseVersionDataImpl(release.version, release.releaseDate) }
                releaseBranch to releaseVersions
            }
    }
}
