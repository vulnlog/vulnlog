package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlBranchBuilder
import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlLifeCycleValue
import dev.vulnlog.dsl.VlPhaseValue
import dev.vulnlog.dsl.VlReleasePublishedValue
import dev.vulnlog.dsl.VlReleaseValue

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
