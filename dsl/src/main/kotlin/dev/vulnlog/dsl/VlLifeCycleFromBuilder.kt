package dev.vulnlog.dsl

interface VlLifeCycleFromBuilder {
    /**
     * Defines the start of a life cycle based on the date of the initial release of the product branch.
     *
     * @return a life cycle phase ending builder.
     */
    fun fromInitRelease(): VlLifeCycleToBuilder

    /**
     * Defines the start of a life cycle based on the end date of the provided life cycle phase.
     *
     * @param lifeCyclePhase that specifies the start of this life cycle phase.
     * @return a life cycle phase ending builder.
     */
    infix fun from(lifeCyclePhase: VlLifeCycleToBuilder): VlLifeCycleToBuilder
}
