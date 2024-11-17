package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlLifeCycleToBuilder

internal class VlLifeCycleToBuilderImpl(private val lifeCycleFrom: VlLifeCycleFromBuilderImpl) : VlLifeCycleToBuilder {
    private var additionalMonths: Long = 0

    override fun addMonths(numberOfMonths: Long): VlLifeCycleToBuilder {
        additionalMonths += numberOfMonths
        return this
    }

    override fun build(): VlLifeCycleTime {
        return VlLifeCycleTime(lifeCycleFrom.lifeCycleName, additionalMonths)
    }
}
