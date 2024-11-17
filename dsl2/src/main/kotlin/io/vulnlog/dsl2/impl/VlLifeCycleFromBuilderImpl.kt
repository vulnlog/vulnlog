package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlLifeCycleFromBuilder
import io.vulnlog.dsl2.VlLifeCycleToBuilder

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
