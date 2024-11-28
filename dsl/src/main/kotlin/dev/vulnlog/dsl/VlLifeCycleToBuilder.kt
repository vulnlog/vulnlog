package dev.vulnlog.dsl

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
