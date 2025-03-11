package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlReleaseContext
import dev.vulnlog.dsl.VlReleasesDslRoot

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
