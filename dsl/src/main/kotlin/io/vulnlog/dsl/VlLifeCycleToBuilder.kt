package io.vulnlog.dsl

import io.vulnlog.dsl.impl.VlLifeCycleTime

interface VlLifeCycleToBuilder {
    /**
     * Defines the end date of a life cycle in months.
     *
     * @param numberOfMonths this life cycle lasts for.
     * @return a life cycle ending builder.
     */
    infix fun addMonths(numberOfMonths: Long): VlLifeCycleToBuilder

    fun build(): VlLifeCycleTime
}
