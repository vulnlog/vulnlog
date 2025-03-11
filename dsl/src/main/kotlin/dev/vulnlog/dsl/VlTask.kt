package dev.vulnlog.dsl

import kotlin.time.Duration

data class TaskData(val analysisData: AnalysisData, val tasks: List<Task>)

class TaskBuilder(val analysisData: AnalysisData) {
    var dependencyName: String? = null
    var action: TaskAction? = null
    val tasks: MutableList<Task> = mutableListOf()

    fun build(): ExecutionBuilder {
        return ExecutionBuilder(TaskData(analysisData, tasks))
    }
}

interface VlTaskInitStep {
    /**
     * The dependency to update as string.
     *
     * @since v0.5.0
     */
    infix fun update(dependency: String): VlTaskUpdateStep

    /**
     * A Release specifier.
     *
     * @since v0.5.0
     */
    infix fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * Duration to wait fore, e.g. `14.days`
     *
     * @since v0.5.0
     */
    infix fun waitOnAllFor(duration: Duration): VlExecutionInitStep
}

interface VlTaskUpdateStep {
    /**
     * Update at least to the specified version.
     *
     * @since v0.5.0
     */
    infix fun atLeastTo(version: String): VlTaskOnStep
}

interface VlTaskFollowUpSpecificationStep {
    /**
     * No further action required on specified release branch range.
     *
     * @since v0.5.0
     */
    infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep

    /**
     * No further action required on specified release branch.
     *
     * @since v0.5.0
     */
    infix fun andNoActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * Update at least to the specified version.
     *
     * @since v0.5.0
     */
    infix fun andUpdateAtLeastTo(version: String): VlTaskOnStep
}

interface VlTaskOnStep {
    /**
     * A Release specifier.
     */
    infix fun on(releaseGroup: ReleaseGroup): VlTaskFollowUpSpecificationStep

    /**
     * A range of release branches e.g. `v1..v2`
     */
    infix fun on(releases: ClosedRange<ReleaseBranch>): VlTaskFollowUpSpecificationStep

    /**
     * A release branches e.g. `v1`
     */
    infix fun on(release: ReleaseBranch): VlTaskFollowUpSpecificationStep
}

sealed interface TaskAction

data object NoActionAction : TaskAction

data class UpdateAction(val dependency: String, val version: String) : TaskAction

data class WaitAction(val forAmountOfTime: Duration) : TaskAction

data class Task(val taskAction: TaskAction, val releases: List<ReleaseBranch>)

sealed interface VulnlogTaskData {
    val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>
}

data class VulnlogTaskDataImpl(override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>) :
    VulnlogTaskData

object VulnlogTaskDataEmpty : VulnlogTaskData {
    override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>> = emptyMap()

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}
