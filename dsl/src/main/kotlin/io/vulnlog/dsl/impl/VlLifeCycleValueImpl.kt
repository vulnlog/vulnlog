package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlLifeCycleToBuilder
import io.vulnlog.dsl.VlLifeCycleValue

internal class VlLifeCycleValueImpl(override val lifeCycles: List<VlLifeCycleToBuilder>) : VlLifeCycleValue
