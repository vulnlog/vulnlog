package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReportFor
import dev.vulnlog.dsl2.VlReportForContext
import dev.vulnlog.dsl2.VlReportedRelease
import dev.vulnlog.dsl2.VlReporter
import java.time.LocalDate

class VlReportForContextImpl(
    private val reporter: VlReporter,
    private val at: LocalDate,
    private val potentiallyEffectedRelease: VlReleaseBranch,
) : VlReportForContextBuilder {
    private val releaseBranchToReleases: MutableMap<String, VlReportedRelease> = mutableMapOf()
    private var lastVariant: String? = null

    override infix fun andExact(release: VlReleaseBranch): VlReportForContext {
        val potentiallyEffectedRelease = VlReportedRelease(release.releaseJustBefore(at))
        releaseBranchToReleases[release.name] = potentiallyEffectedRelease
        return this
    }

    override infix fun to(release: VlReleaseBranch): VlReportForContext {
        val releaseBranches = mutableListOf<VlReleaseBranch>()
        releaseBranches += release
        var previousRelease = release.predecessor
        while (previousRelease != null && previousRelease != potentiallyEffectedRelease) {
            releaseBranches += previousRelease
            previousRelease = previousRelease.predecessor
        }
        releaseBranches += potentiallyEffectedRelease
        releaseBranches.map { releaseBranchToReleases[it.name] = VlReportedRelease(it.releaseJustBefore(at)) }
        return this
    }

    override infix fun onVariant(variant: String): VlReportForContext {
        lastVariant = variant
        return this
    }

    override fun build(): VlReportFor {
        return VlReportFor(reporter, at, releaseBranchToReleases)
    }
}
