package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VlReleaseContext
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlReporterContext
import dev.vulnlog.dslinterpreter.repository.BranchRepository
import dev.vulnlog.dslinterpreter.repository.ReporterRepository

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

    override fun reporters(block: (VlReporterContext).() -> Unit): Unit =
        with(VlReporterContextImpl()) {
            block()
            val reporterData = reporters.map { ReporterDataImpl(it.name) }
            this@VlReleasesDslRootImpl.reporterDataRepository.addAll(reporterData)
        }
}
