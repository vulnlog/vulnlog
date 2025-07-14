package dev.vulnlog.dsl

import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public val Int.days: Duration
    get() = toDuration(DurationUnit.DAYS)

public sealed interface VulnlogExecution {
    public val action: String
    public val releases: List<ReleaseBranchData>
}

public data class VulnlogSuppressUntilExecution(
    override val action: String,
    override val releases: List<ReleaseBranchData>,
    val untilDate: LocalDate,
) : VulnlogExecution

public data class VulnlogSuppressUntilNextPublicationExecution(
    override val action: String,
    override val releases: List<ReleaseBranchData>,
    val involved: Map<ReleaseBranchData, InvolvedReleaseVersion>,
) : VulnlogExecution

public data class VulnlogSuppressPermanentExecution(
    override val action: String,
    override val releases: List<ReleaseBranchData>,
) : VulnlogExecution

public data class VulnlogFixExecution(
    override val action: String,
    override val releases: List<ReleaseBranchData>,
    val fixDate: LocalDate,
) : VulnlogExecution

/**
 * Represents the initial state of an execution in the vulnerability lifecycle management DSL.
 * This interface provides methods to suppress or resolve vulnerabilities during the DSL workflow.
 */
public interface VlExecutionInitState {
    /**
     * Suppress a vulnerability permanently.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: VlSuppressionPermanent): VlExecutionOnState

    /**
     * Suppress a vulnerability for a certain amount of time.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: VlSuppressionTemporarily): VlExecutionSuppressTemporarilyState

    /**
     * Suppress a vulnerability until the next release is published.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: VlSuppressionUntilNextPublication): VlExecutionOnState

    /**
     * Mark a vulnerability as fixed at [date] e.g. `2025-03-07`.
     *
     * @since v0.7.0
     */
    public infix fun fixedAt(date: String): VlExecutionOnState
}

/**
 * Represents the transition state for executing actions on specific releases or release groups
 * within the vulnerability lifecycle management DSL.
 *
 * This interface provides methods to specify the target releases or release groups where the defined
 * actions will be applied.
 */
public interface VlExecutionOnState {
    /**
     * Describe on what [releaseGroup] this execution applies.
     *
     * @since v0.5.0
     */
    public infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitState

    /**
     * Describe on what [releases] this execution applies.
     *
     * @since v0.5.0
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitState

    /**
     * Describe on what [release] this execution applies.
     *
     * @since v0.5.0
     */
    public infix fun on(release: ReleaseBranch): VlExecutionInitState
}

/**
 * Represents a state in the vulnerability lifecycle management DSL where execution is temporarily suppressed.
 * This interface allows specifying a waiting duration before transitioning to the next state.
 */
public interface VlExecutionSuppressTemporarilyState {
    /**
     * Duration to wait fore, e.g. `14.days`
     *
     * @since v0.5.0
     */
    public infix fun forTime(duration: Duration): VlExecutionOnState
}
