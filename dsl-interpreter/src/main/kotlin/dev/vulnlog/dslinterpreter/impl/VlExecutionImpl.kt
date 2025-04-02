package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.ExecutionOnStep
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.SuppressionSpecifierPermanent
import dev.vulnlog.dsl.SuppressionSpecifierTemporarily
import dev.vulnlog.dsl.SuppressionSpecifierUntilNextPublication
import dev.vulnlog.dsl.VlExecutionInitStep
import dev.vulnlog.dsl.VlExecutionSuppressTemporarilyStep
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

class VlExecutionInitStepImpl(private val executionBuilder: Lazy<ExecutionBuilder>) : VlExecutionInitStep {
    override infix fun suppress(specifier: SuppressionSpecifierPermanent): ExecutionOnStep {
        val suppress =
            { releaseBranches: List<ReleaseBranch> -> SuppressionPermanentExecution(releases = releaseBranches) }
        return ExecutionOnStepImpl(this, executionBuilder.value, suppress)
    }

    override infix fun suppress(specifier: SuppressionSpecifierTemporarily): VlExecutionSuppressTemporarilyStep {
        return VlExecutionSuppressTemporarilyStepImpl(this, executionBuilder.value)
    }

    override infix fun suppress(specifier: SuppressionSpecifierUntilNextPublication): ExecutionOnStep {
        val suppress =
            { releaseBranches: List<ReleaseBranch> -> SuppressionEventExecution(releases = releaseBranches) }
        return ExecutionOnStepImpl(this, executionBuilder.value, suppress)
    }

    override fun fixedAt(date: String): ExecutionOnStep {
        val fix = { releaseBranches: List<ReleaseBranch> ->
            FixedExecution(
                fixDate = LocalDate.parse(date),
                releases = releaseBranches,
            )
        }
        return ExecutionOnStepImpl(this, executionBuilder.value, fix)
    }
}

class ExecutionOnStepImpl(
    private val vlExecutionInitStep: VlExecutionInitStep,
    private val executionBuilder: ExecutionBuilder,
    private val executionLambda: (List<ReleaseBranch>) -> Execution,
) : ExecutionOnStep {
    override infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitStep {
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
        return vlExecutionInitStep
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep {
        val releaseList = allReleases().filter { it in releases }
        val execution: Execution = executionLambda.invoke(releaseList)
        executionBuilder.executions += execution
        return vlExecutionInitStep
    }

    override infix fun on(release: ReleaseBranch): VlExecutionInitStep {
        val execution: Execution = executionLambda.invoke(listOf(release))
        executionBuilder.executions += execution
        return vlExecutionInitStep
    }
}

class VlExecutionSuppressTemporarilyStepImpl(
    private val vlExecutionInitStepImpl: VlExecutionInitStepImpl,
    private val executionBuilder: ExecutionBuilder,
) : VlExecutionSuppressTemporarilyStep {
    override infix fun forTime(duration: Duration): ExecutionOnStep {
        val suppressUntil: LocalDate =
            executionBuilder.dslTaskData.analysisData?.analysedAt?.plusDays(duration.inWholeDays)!!
        val suppression = { releaseBranches: List<ReleaseBranch> ->
            SuppressionDateExecution(suppressUntilDate = suppressUntil, releases = releaseBranches)
        }
        return ExecutionOnStepImpl(vlExecutionInitStepImpl, executionBuilder, suppression)
    }
}

data class VulnlogExecutionDataImpl(override val executions: List<VulnlogExecution>) : VulnlogExecutionData
