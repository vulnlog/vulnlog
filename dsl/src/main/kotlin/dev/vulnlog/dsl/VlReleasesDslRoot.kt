package dev.vulnlog.dsl

import dev.vulnlog.dsl.definition.VlDslMarker

public interface VlReleasesDslRoot {
    public val branchToReleases: Map<ReleaseBranchData, List<ReleaseVersionData>>

    /**
     * Define releases.
     *
     * @since v0.5.0
     */
    public fun releases(block: (@VlDslMarker VlReleaseContext).() -> Unit)
}
