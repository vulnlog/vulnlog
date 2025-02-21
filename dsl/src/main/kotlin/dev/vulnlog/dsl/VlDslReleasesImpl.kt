package dev.vulnlog.dsl

class VlDslReleasesImpl : VlDslReleases {
    var branchToReleases = mapOf<ReleaseBranchData, List<ReleaseVersionData>>()

    override fun releases(block: VlReleaseContext.() -> Unit) =
        with(VlReleaseContextImpl()) {
            block()
            this@VlDslReleasesImpl.branchToReleases += getBranchToReleases()
        }

    override fun toString(): String {
        return "VlDslReleasesImpl(branchToReleases=$branchToReleases)"
    }
}
