package dev.vulnlog.dsl

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val Int.days: Duration
    get() = toDuration(DurationUnit.DAYS)

data class ExecutionData(val taskData: TaskData, val executions: List<Execution>)

class ExecutionBuilder(val taskData: TaskData) {
    val executions: MutableList<Execution> = mutableListOf()
    var suppressionSpecifier: SuppressionSpecifier? = null

    fun build(): ExecutionData {
        return ExecutionData(taskData, executions)
    }
}

interface VlExecutionInitStep {
    /**
     * Suppress a vulnerability permanently.
     *
     * @since v0.5.0
     */
    infix fun suppress(specifier: SuppressionSpecifierPermanent): ExecutionOnStep

    /**
     * Suppress a vulnerability for a certain amount of time.
     *
     * @since v0.5.0
     */
    infix fun suppress(specifier: SuppressionSpecifierTemporarily): VlExecutionSuppressTemporarilyStep

    /**
     * Suppress a vulnerability until the next release is published.
     *
     * @since v0.5.0
     */
    infix fun suppress(specifier: SuppressionSpecifierUntilNextPublication): ExecutionOnStep
}

interface ExecutionOnStep {
    /**
     * A Release specifier.
     *
     * @since v0.5.0
     */
    infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitStep

    /**
     * A range of release branches e.g. `v1..v2`
     *
     * @since v0.5.0
     */
    infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep

    /**
     * A release branches e.g. `v1`
     *
     * @since v0.5.0
     */
    infix fun on(release: ReleaseBranch): VlExecutionInitStep
}

interface VlExecutionSuppressTemporarilyStep {
    /**
     * Duration to wait fore, e.g. `14.days`
     *
     * @since v0.5.0
     */
    infix fun forTime(duration: Duration): ExecutionOnStep
}

data class Execution(val action: String, val duration: String, val releases: List<ReleaseBranch>)

data class ExecutionData2(val action: String, val duration: String, val releases: List<ReleaseBranchData>)

sealed interface VulnlogExecutionData {
    val executions: List<ExecutionData2>
}

data class VulnlogExecutionDataImpl(override val executions: List<ExecutionData2>) : VulnlogExecutionData

object VulnlogExecutionDataEmpty : VulnlogExecutionData {
    override val executions: List<ExecutionData2> = emptyList()

    override fun toString(): String {
        return "VulnlogExecutionDataEmpty()"
    }
}
