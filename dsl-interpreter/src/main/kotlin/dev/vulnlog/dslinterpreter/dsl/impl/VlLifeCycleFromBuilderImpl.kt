package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlLifeCycleFromBuilder
import dev.vulnlog.dsl.VlLifeCycleToBuilder

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
