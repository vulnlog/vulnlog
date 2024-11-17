package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlLifeCycleToBuilder
import io.vulnlog.dsl2.VlLifeCycleValue

internal class VlLifeCycleValueImpl(override val lifeCycles: List<VlLifeCycleToBuilder>) : VlLifeCycleValue
