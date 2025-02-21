package dev.vulnlog.dsl

class VlReleasesDslRootImpl : VlReleasesDslRoot {
    override var branchToReleases = mapOf<ReleaseBranchData, List<ReleaseVersionData>>()

    override fun releases(block: VlReleaseContext.() -> Unit) =
        with(VlReleaseContextImpl()) {
            block()
            this@VlReleasesDslRootImpl.branchToReleases += getBranchToReleases()
        }

    override fun toString(): String {
        return "VlDslReleasesImpl(branchToReleases=$branchToReleases)"
    }
}
