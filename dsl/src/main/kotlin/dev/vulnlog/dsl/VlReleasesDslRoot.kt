package dev.vulnlog.dsl

interface VlReleasesDslRoot {
    val branchToReleases: Map<ReleaseBranchData, List<ReleaseVersionData>>

    /**
     * Add release branches.
     */
    fun releases(block: VlReleaseContext.() -> Unit)
}
