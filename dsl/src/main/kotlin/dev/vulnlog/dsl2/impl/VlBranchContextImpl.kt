package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.MyRelease
import dev.vulnlog.dsl2.MyReleaseImpl
import dev.vulnlog.dsl2.VlLifeCyclePhase
import dev.vulnlog.dsl2.VlPublication
import dev.vulnlog.dsl2.VlPublicationDefault
import dev.vulnlog.dsl2.VlPublicationImpl
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReleaseBranchImpl
import dev.vulnlog.dsl2.VlVersion
import dev.vulnlog.dsl2.VlVersionImpl
import java.time.LocalDate

class VlBranchContextImpl(private val name: String, private val lifeCyclePhase: List<VlLifeCyclePhase>) :
    VlBranchContextBuilder {
    private val releases: MutableList<MyRelease> = mutableListOf()

    override infix fun String.publishedAt(publication: String): Pair<VlVersion, VlPublication> {
        if (publication.isBlank()) return VlVersionImpl(this) to VlPublicationDefault
        return VlVersionImpl(this) to VlPublicationImpl(LocalDate.parse(publication))
    }

    override fun release(vararg release: Pair<VlVersion, VlPublication>) {
        releases.addAll(release.map { MyReleaseImpl(it.first, it.second) })
    }

    override fun build(): VlReleaseBranch {
        return VlReleaseBranchImpl(name, lifeCyclePhase, releases)
    }

    override fun build(predecessor: VlReleaseBranch?): VlReleaseBranch {
        return VlReleaseBranchImpl(name, lifeCyclePhase, releases, predecessor)
    }
}
