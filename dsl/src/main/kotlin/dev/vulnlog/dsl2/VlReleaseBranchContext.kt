package dev.vulnlog.dsl2

interface VlReleaseBranchContext {
    fun branch(
        name: String,
        vararg lifeCyclePhases: VlLifeCyclePhase = emptyArray(),
        block: VlBranchContext.() -> Unit,
    )

    fun lifeCycles(vararg lifeCyclesPhases: VlLifeCyclePhase): Array<VlLifeCyclePhase>

    infix fun String.months(months: Long): VlLifeCyclePhase = VlLifeCyclePhase(this, months)
}
