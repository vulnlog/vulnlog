package dev.vulnlog.dsl2

sealed interface MyResolutionTask {
    val taskDescription: String
    val rating: VlVulnerabilityRateContext
    val applyOn: String
}

data class MyUpdateResolutionTask(
    override val rating: VlVulnerabilityRateContext,
    override val applyOn: String,
) : MyResolutionTask {
    override val taskDescription: String = "update"

    infix fun atLeastTo(version: String): MyResolutionTaskTarget {
        return MyResolutionTaskTarget(this, version)
    }
}

sealed interface MyResolution {
    val resolutionTask: MyResolutionTask
    val version: String
    val releaseBranches: MutableList<VlReleaseBranch>
}

class MyResolutionTaskTarget(
    override val resolutionTask: MyResolutionTask,
    override val version: String,
    override val releaseBranches: MutableList<VlReleaseBranch> = mutableListOf(),
) : MyResolution {
    infix fun onRelease(releaseBranch: VlReleaseBranch): MyResolutionTaskTargetContinuation {
        releaseBranches += releaseBranch
        return MyResolutionTaskTargetContinuation(resolutionTask, version, releaseBranches)
    }
}

class MyResolutionTaskTargetContinuation(
    override val resolutionTask: MyResolutionTask,
    override val version: String,
    override val releaseBranches: MutableList<VlReleaseBranch>,
) : MyResolution {
    infix fun andOn(releaseBranch: VlReleaseBranch): MyResolutionTaskTargetContinuation {
        releaseBranches += releaseBranch
        return this
    }
}
