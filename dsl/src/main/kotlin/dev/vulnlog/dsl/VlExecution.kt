package dev.vulnlog.dsl

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public val Int.days: Duration
    get() = toDuration(DurationUnit.DAYS)

public interface VulnlogExecutionData {
    public val executions: List<VulnlogExecution>
}

public data class VulnlogExecution(val action: String, val duration: String, val releases: List<ReleaseBranchData>)

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
