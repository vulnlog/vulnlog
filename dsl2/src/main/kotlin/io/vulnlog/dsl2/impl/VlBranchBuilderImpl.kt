package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlBranchValue
import io.vulnlog.dsl2.VlLifeCycleValue
import io.vulnlog.dsl2.VlPhaseValue
import io.vulnlog.dsl2.VlReleasePublishedValue
import io.vulnlog.dsl2.VlReleaseValue

internal class VlBranchBuilderImpl(
    private val name: String,
    private val initialVersion: VlReleaseValue,
    private val lifeCycle: VlLifeCycleValue,
) :
    VlBranchBuilder {
    private val releases: MutableList<VlReleaseValue> = mutableListOf()

    init {
        releases += initialVersion
    }

    override fun supersededBy(release: VlReleaseValue): VlBranchBuilder {
        releases += release
        return this
    }

    fun build(): VlBranchValue {
        val phases: MutableList<VlPhaseValue> = mutableListOf()
        if (initialVersion is VlReleasePublishedValue) {
            val initReleaseDate = initialVersion.releaseDate
            var currentLifeCycle: VlLifeCycleTime = lifeCycle.lifeCycles[0].build()
            var aPhase =
                VlPhaseValueImpl(
                    currentLifeCycle.lifeCycleName,
                    Pair(initReleaseDate, initReleaseDate.plusMonths(currentLifeCycle.months)),
                )
            phases += aPhase

            for (i in 1 until lifeCycle.lifeCycles.size) {
                currentLifeCycle = lifeCycle.lifeCycles[i].build()

                aPhase =
                    VlPhaseValueImpl(
                        currentLifeCycle.lifeCycleName,
                        Pair(
                            aPhase.phaseDuration.second,
                            aPhase.phaseDuration.second.plusMonths(currentLifeCycle.months),
                        ),
                    )
                phases += aPhase
            }
        } else {
            // to nothing if the initial release does not already has a publication date
        }
        return VlBranchValueImpl(name, initialVersion, releases, phases)
    }
}
