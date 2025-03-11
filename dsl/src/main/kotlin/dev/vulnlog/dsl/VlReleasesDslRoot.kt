package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

interface VlReleasesDslRoot {
    val branchToReleases: Map<ReleaseBranchData, List<ReleaseVersionData>>

    /**
     * Define releases.
     *
     * @since v0.5.0
     */
    fun releases(block: (@VlDslMarker VlReleaseContext).() -> Unit)
}
