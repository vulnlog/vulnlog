package io.vulnlog.dsl2

interface VlLifeCycle {
    /**
     * Create a product branch life cycle phase describing a relative time range a product branch is a life.
     *
     * @param name of the life cycle phase.
     */
    fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder

    /**
     * Create a complete product branch life cycle of multiple life cycle phases.
     *
     * @param lifeCyclePhases specifies the sequential phases this life cycle contains.
     * @return complete product branch life cycle.
     */
    fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycleValue
}
