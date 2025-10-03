package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.common.repository.ReporterRepository
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlReleaseContext
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlReporterImpl
import dev.vulnlog.dsl.VlReportersContext

class VlReleasesDslRootImpl(
    private val branchRepository: BranchRepository,
    private val reporterDataRepository: ReporterRepository,
) : VlReleasesDslRoot {
    override fun releases(block: VlReleaseContext.() -> Unit) =
        with(VlReleaseContextImpl()) {
            block()
            val branchToReleases: Map<ReleaseBranchData, List<ReleaseVersionData>> = getBranchToReleases()
            branchToReleases.forEach { (branch, releaseVersions) ->
                this@VlReleasesDslRootImpl.branchRepository.add(
                    branch,
                    releaseVersions,
                )
            }
        }

    override fun reporters(block: (VlReportersContext).() -> Unit): Unit =
        with(VlReportersContextImpl()) {
            block()
            val reporterData =
                reporters
                    .filterIsInstance<VlReporterImpl>()
                    .map { ReporterDataImpl(it.name, it.config) }
            this@VlReleasesDslRootImpl.reporterDataRepository.addAll(reporterData)
        }
}
