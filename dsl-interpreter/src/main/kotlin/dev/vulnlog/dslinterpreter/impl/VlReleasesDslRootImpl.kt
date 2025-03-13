package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.ReporterData
import dev.vulnlog.dsl.ReporterDataImpl
import dev.vulnlog.dsl.VlReleaseContext
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlReporterContext

class VlReleasesDslRootImpl : VlReleasesDslRoot {
    override var branchToReleases = mapOf<ReleaseBranchData, List<ReleaseVersionData>>()
    override var reporters = listOf<ReporterData>()

    override fun releases(block: VlReleaseContext.() -> Unit) =
        with(VlReleaseContextImpl()) {
            block()
            this@VlReleasesDslRootImpl.branchToReleases += getBranchToReleases()
        }

    override fun reporters(block: (VlReporterContext).() -> Unit) =
        with(VlReporterContextImpl()) {
            block()
            this@VlReleasesDslRootImpl.reporters = reporters.map { ReporterDataImpl(it.name) }
        }

    override fun toString(): String {
        return "VlDslReleasesImpl(branchToReleases=$branchToReleases)"
    }
}
