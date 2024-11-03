package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlLifeCycleToBuilder

internal class VlLifeCycleToBuilderImpl : VlLifeCycleToBuilder {
    override fun addMonths(numberOfMonths: Int): VlLifeCycleToBuilder = VlLifeCycleToBuilderImpl()
}
