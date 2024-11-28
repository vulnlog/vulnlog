package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlLifeCycleTime

data class VlLifeCycleTimeValue(override val lifeCycleName: String, override val months: Long = 0) : VlLifeCycleTime
