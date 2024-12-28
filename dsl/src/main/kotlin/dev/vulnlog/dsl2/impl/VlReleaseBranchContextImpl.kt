package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlBranchContext
import dev.vulnlog.dsl2.VlLifeCyclePhase
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReleaseBranchContext

class VlReleaseBranchContextImpl : VlReleaseBranchContext, VlReleaseBranchContextBuilder {
    private val branches: MutableList<VlReleaseBranch> = mutableListOf()

    override fun branch(
        name: String,
        vararg lifeCyclePhases: VlLifeCyclePhase,
        block: VlBranchContext.() -> Unit,
    ) = with(VlBranchContextImpl(name, lifeCyclePhases.toList())) {
        block()
        branches += if (branches.isNotEmpty()) build(branches.last()) else build()
    }

    override fun lifeCycles(vararg lifeCyclesPhases: VlLifeCyclePhase): Array<VlLifeCyclePhase> =
        lifeCyclesPhases.toList().toTypedArray()

    override fun build(): Array<VlReleaseBranch> = branches.toTypedArray()
}
