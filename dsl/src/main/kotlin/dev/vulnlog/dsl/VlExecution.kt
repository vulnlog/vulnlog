package dev.vulnlog.dsl

import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public val Int.days: Duration
    get() = toDuration(DurationUnit.DAYS)

public interface VulnlogExecutionData {
    public val executions: List<VulnlogExecution>
}

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

public interface VlExecutionInitStep {
    /**
     * Suppress a vulnerability permanently.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: SuppressionSpecifierPermanent): ExecutionOnStep

    /**
     * Suppress a vulnerability for a certain amount of time.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: SuppressionSpecifierTemporarily): VlExecutionSuppressTemporarilyStep

    /**
     * Suppress a vulnerability until the next release is published.
     *
     * @since v0.5.0
     */
    public infix fun suppress(specifier: SuppressionSpecifierUntilNextPublication): ExecutionOnStep

    /**
     * Mark a vulnerability as fixed.
     *
     * @since v0.7.0
     */
    public infix fun fixedAt(date: String): ExecutionOnStep
}

public interface ExecutionOnStep {
    /**
     * A Release specifier.
     *
     * @since v0.5.0
     */
    public infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * A range of release branches e.g. `v1..v2`
     *
     * @since v0.5.0
     */
    public infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep

    /**
     * A release branches e.g. `v1`
     *
     * @since v0.5.0
     */
    public infix fun on(release: ReleaseBranch): VlExecutionInitStep
}

public interface VlExecutionSuppressTemporarilyStep {
    /**
     * Duration to wait fore, e.g. `14.days`
     *
     * @since v0.5.0
     */
    public infix fun forTime(duration: Duration): ExecutionOnStep
}
