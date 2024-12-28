package dev.vulnlog.dsl2

data class VlResolutionTask(
    val resolutionTask: MyResolutionTask,
    val version: String,
    val releaseBranches: List<VlReleaseBranch>,
)
