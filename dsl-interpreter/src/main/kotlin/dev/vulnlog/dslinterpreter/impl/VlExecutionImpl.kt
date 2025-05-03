package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.VlExecutionInitState
import dev.vulnlog.dsl.VlExecutionOnState
import dev.vulnlog.dsl.VlExecutionSuppressTemporarilyState
import dev.vulnlog.dsl.VlSuppressionPermanent
import dev.vulnlog.dsl.VlSuppressionTemporarily
import dev.vulnlog.dsl.VlSuppressionUntilNextPublication
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogExecutionData
import java.time.LocalDate
import kotlin.time.Duration

data class DslExecutionData(val dslTaskData: DslTaskData, val executions: List<Execution>)

sealed interface Execution {
    val action: String
    val releases: List<ReleaseBranch>
}

data class SuppressionPermanentExecution(
    override val action: String = "suppress",
    override val releases: List<ReleaseBranch>,
) : Execution

data class SuppressionEventExecution(
    override val action: String = "suppress",
    override val releases: List<ReleaseBranch>,
) : Execution

data class SuppressionDateExecution(
    override val action: String = "suppress",
    override val releases: List<ReleaseBranch>,
    val suppressUntilDate: LocalDate,
) : Execution

data class FixedExecution(
    override val action: String = "fix",
    val fixDate: LocalDate,
    override val releases: List<ReleaseBranch>,
) : Execution

class ExecutionBuilder(val dslTaskData: DslTaskData) {
    val executions: MutableList<Execution> = mutableListOf()

    fun build(): DslExecutionData {
        return DslExecutionData(dslTaskData, executions)
    }
}

class VlExecutionInitStateImpl(private val executionBuilder: Lazy<ExecutionBuilder>) : VlExecutionInitState {
    override infix fun suppress(specifier: VlSuppressionPermanent): VlExecutionOnState {
        val suppress =
            { releaseBranches: List<ReleaseBranch> -> SuppressionPermanentExecution(releases = releaseBranches) }
        return VlExecutionOnStateImpl(this, executionBuilder.value, suppress)
    }

    override infix fun suppress(specifier: VlSuppressionTemporarily): VlExecutionSuppressTemporarilyState {
        return VlExecutionSuppressTemporarilyStateImpl(this, executionBuilder.value)
    }

    override infix fun suppress(specifier: VlSuppressionUntilNextPublication): VlExecutionOnState {
        val suppress =
            { releaseBranches: List<ReleaseBranch> -> SuppressionEventExecution(releases = releaseBranches) }
        return VlExecutionOnStateImpl(this, executionBuilder.value, suppress)
    }

    override fun fixedAt(date: String): VlExecutionOnState {
        val fix = { releaseBranches: List<ReleaseBranch> ->
            FixedExecution(
                fixDate = LocalDate.parse(date),
                releases = releaseBranches,
            )
        }
        return VlExecutionOnStateImpl(this, executionBuilder.value, fix)
    }
}

class VlExecutionOnStateImpl(
    private val vlExecutionInitState: VlExecutionInitState,
    private val executionBuilder: ExecutionBuilder,
    private val executionLambda: (List<ReleaseBranch>) -> Execution,
) : VlExecutionOnState {
    override infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitState {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        executionBuilder.executions.flatMap { it.releases }.contains(a)
                    }
            }
        val execution: Execution = executionLambda.invoke(releaseList)
        executionBuilder.executions += execution
        return vlExecutionInitState
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitState {
        val releaseList = allReleases().filter { it in releases }
        val execution: Execution = executionLambda.invoke(releaseList)
        executionBuilder.executions += execution
        return vlExecutionInitState
    }

    override infix fun on(release: ReleaseBranch): VlExecutionInitState {
        val execution: Execution = executionLambda.invoke(listOf(release))
        executionBuilder.executions += execution
        return vlExecutionInitState
    }
}

class VlExecutionSuppressTemporarilyStateImpl(
    private val vlExecutionInitStateImpl: VlExecutionInitStateImpl,
    private val executionBuilder: ExecutionBuilder,
) : VlExecutionSuppressTemporarilyState {
    override infix fun forTime(duration: Duration): VlExecutionOnState {
        val suppressUntil: LocalDate =
            executionBuilder.dslTaskData.analysisData?.analysedAt?.plusDays(duration.inWholeDays)!!
        val suppression = { releaseBranches: List<ReleaseBranch> ->
            SuppressionDateExecution(suppressUntilDate = suppressUntil, releases = releaseBranches)
        }
        return VlExecutionOnStateImpl(vlExecutionInitStateImpl, executionBuilder, suppression)
    }
}

data class VulnlogExecutionDataImpl(override val executions: List<VulnlogExecution>) : VulnlogExecutionData
