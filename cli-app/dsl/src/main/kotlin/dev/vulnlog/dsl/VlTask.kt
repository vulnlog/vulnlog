package dev.vulnlog.dsl

import kotlin.time.Duration

public interface VulnlogTaskData {
    public val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>
}

/**
 * Represents the initial state of a task in the vulnerability task management DSL.
 * It serves as the starting point for defining task initialization actions within the DSL.
 */
public interface VlTaskInitState {
    /**
     * The `dependency` to update as string.
     *
     * @since v0.5.0
     */
    public infix fun update(dependency: String): VlTaskUpdateState

    /**
     * No actions required on [releaseGroup].
     *
     * @since v0.5.0
     */
    public infix fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitState

    /**
     * Wait on all release branches for [duration], e.g., 14.days.
     *
     * @since v0.5.0
     */
    public infix fun waitOnAllFor(duration: Duration): VlExecutionInitState
}

/**
 * Represents an interface for updating the state of a vulnerability task to a specific version.
 *
 * This interface is utilized within the vulnerability reporting DSL to define and manage version updates
 * for tasks in a fluent and declarative manner.
 */
public interface VlTaskUpdateState {
    /**
     * Update at least to the specified [version].
     *
     * @since v0.5.0
     */
    public infix fun atLeastTo(version: String): VlTaskOnState
}

/**
 * Represents the follow-up state of a task in the vulnerability lifecycle management DSL,
 * allowing further actions to be defined on specific release branches or release groups.
 */
public interface VlTaskFollowState {
    /**
     * No further action required on [releases].
     *
     * @since v0.5.0
     */
    public infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): VlExecutionInitState

    /**
     * No further action required on [releaseGroup].
     *
     * @since v0.5.0
     */
    public infix fun andNoActionOn(releaseGroup: ReleaseGroup): VlExecutionInitState

    /**
     * Update at least to the specified [version].
     *
     * @since v0.5.0
     */
    public infix fun andUpdateAtLeastTo(version: String): VlTaskOnState
}

/**
 * Represents the task-on-state within the vulnerability lifecycle management DSL,
 * allowing the specification of actions and follow-up steps on certain release branches or groups.
 */
public interface VlTaskOnState {
    /**
     * Describe on what [releaseGroup] this task applies.
     */
    public infix fun on(releaseGroup: ReleaseGroup): VlTaskFollowState

    /**
     * Describe on what [releases] this task applies.
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlTaskFollowState

    /**
     * Describe on what [release] this task applies.
     */
    public infix fun on(release: ReleaseBranch): VlTaskFollowState
}

public sealed interface TaskAction

public data object NoActionAction : TaskAction

public data class UpdateAction(val dependency: String, val version: String) : TaskAction

public data class WaitAction(val forAmountOfTime: Duration) : TaskAction
