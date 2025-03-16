package dev.vulnlog.dsl

import kotlin.time.Duration

public data class TaskData(val analysisData: AnalysisData?, val tasks: List<Task>)

public interface VlTaskInitStep {
    /**
     * The dependency to update as string.
     *
     * @since v0.5.0
     */
    public infix fun update(dependency: String): VlTaskUpdateStep

    /**
     * A Release specifier.
     *
     * @since v0.5.0
     */
    public infix fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * Duration to wait fore, e.g. `14.days`
     *
     * @since v0.5.0
     */
    public infix fun waitOnAllFor(duration: Duration): VlExecutionInitStep
}

public interface VlTaskUpdateStep {
    /**
     * Update at least to the specified version.
     *
     * @since v0.5.0
     */
    public infix fun atLeastTo(version: String): VlTaskOnStep
}

public interface VlTaskFollowUpSpecificationStep {
    /**
     * No further action required on specified release branch range.
     *
     * @since v0.5.0
     */
    public infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep

    /**
     * No further action required on specified release branch.
     *
     * @since v0.5.0
     */
    public infix fun andNoActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * Update at least to the specified version.
     *
     * @since v0.5.0
     */
    public infix fun andUpdateAtLeastTo(version: String): VlTaskOnStep
}

public interface VlTaskOnStep {
    /**
     * A Release specifier.
     */
    public infix fun on(releaseGroup: ReleaseGroup): VlTaskFollowUpSpecificationStep

    /**
     * A range of release branches e.g. `v1..v2`
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlTaskFollowUpSpecificationStep

    /**
     * A release branches e.g. `v1`
     */
    public infix fun on(release: ReleaseBranch): VlTaskFollowUpSpecificationStep
}

public sealed interface TaskAction

public data object NoActionAction : TaskAction

public data class UpdateAction(val dependency: String, val version: String) : TaskAction

public data class WaitAction(val forAmountOfTime: Duration) : TaskAction

public data class Task(val taskAction: TaskAction, val releases: List<ReleaseBranch>)

public interface VulnlogTaskData {
    public val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>
}
