package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.DefaultReleaseBranchDataImpl
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseBranchDataImpl
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.ReleaseVersionDataImpl
import dev.vulnlog.dsl.VlBranchContext
import dev.vulnlog.dsl.VlRelease
import dev.vulnlog.dsl.VlRelease.Companion.createRelease
import dev.vulnlog.dsl.VlReleaseContext

class VlReleaseContextImpl : VlReleaseContext {
    private val defaultReleaseBranch = ReleaseBranch.create("default release branch")
    private val defaultReleasesVersion = mutableListOf<VlRelease>()
    private val branchToReleases = mutableMapOf(defaultReleaseBranch to defaultReleasesVersion)

    // TODO diese methode wird fuer publicationDate == null verwendet, das ist nicht korrekt
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
