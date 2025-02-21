package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

interface VlReleasesDslRoot {
    val branchToReleases: Map<ReleaseBranchData, List<ReleaseVersionData>>

    /**
     * Add release branches.
     */
    fun releases(block: (@VlDslMarker VlReleaseContext).() -> Unit)
}
