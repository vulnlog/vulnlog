package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlLifeCycleFromBuilder
import io.vulnlog.dsl2.VlLifeCycleToBuilder
import io.vulnlog.dsl2.data.VlLifeCycleData

internal class VlLifeCycleFromBuilderImpl : VlLifeCycleFromBuilder {
    override fun fromInitRelease(): VlLifeCycleToBuilder = VlLifeCycleToBuilderImpl()

    override fun from(lifeCyclePhase: VlLifeCycleToBuilder): VlLifeCycleToBuilder = VlLifeCycleToBuilderImpl()

    fun build(): VlLifeCycleData = VlLifeCycleData("TODO")
}
