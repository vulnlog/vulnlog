package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlLifeCycleToBuilder
import dev.vulnlog.dsl.VlLifeCycleValue

internal class VlLifeCycleValueImpl(override val lifeCycles: List<VlLifeCycleToBuilder>) : VlLifeCycleValue
