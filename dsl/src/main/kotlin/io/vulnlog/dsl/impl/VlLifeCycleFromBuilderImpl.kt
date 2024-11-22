package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlLifeCycleFromBuilder
import io.vulnlog.dsl.VlLifeCycleToBuilder

internal class VlLifeCycleFromBuilderImpl(val lifeCycleName: String) : VlLifeCycleFromBuilder {
    private lateinit var lifeCycleToBuilder: VlLifeCycleToBuilder

    override fun fromInitRelease(): VlLifeCycleToBuilder {
        lifeCycleToBuilder = VlLifeCycleToBuilderImpl(this)
        return lifeCycleToBuilder
    }

    override fun from(lifeCyclePhase: VlLifeCycleToBuilder): VlLifeCycleToBuilder {
        lifeCycleToBuilder = VlLifeCycleToBuilderImpl(this)
        return lifeCycleToBuilder
    }
}
