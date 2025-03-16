package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.ExecutionOnStep
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.SuppressionSpecifier
import dev.vulnlog.dsl.SuppressionSpecifierPermanent
import dev.vulnlog.dsl.SuppressionSpecifierTemporarily
import dev.vulnlog.dsl.SuppressionSpecifierUntilNextPublication
import dev.vulnlog.dsl.VlExecutionInitStep
import dev.vulnlog.dsl.VlExecutionSuppressTemporarilyStep
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogExecutionData
import kotlin.time.Duration

data class ExecutionData(val taskData: TaskData, val executions: List<Execution>)

data class Execution(val action: String, val duration: String, val releases: List<ReleaseBranch>)

class ExecutionBuilder(val taskData: TaskData) {
    val executions: MutableList<Execution> = mutableListOf()
    var suppressionSpecifier: SuppressionSpecifier? = null

    fun build(): ExecutionData {
        return ExecutionData(taskData, executions)
    }
}

class VlExecutionInitStepImpl(private val executionBuilder: Lazy<ExecutionBuilder>) : VlExecutionInitStep {
    override infix fun suppress(specifier: SuppressionSpecifierPermanent): ExecutionOnStep {
        executionBuilder.value.suppressionSpecifier = specifier
        return ExecutionOnStepImpl(this, executionBuilder.value)
    }

    override infix fun suppress(specifier: SuppressionSpecifierTemporarily): VlExecutionSuppressTemporarilyStep {
        executionBuilder.value.suppressionSpecifier = specifier
        return VlExecutionSuppressTemporarilyStepImpl(this, executionBuilder.value)
    }

    override infix fun suppress(specifier: SuppressionSpecifierUntilNextPublication): ExecutionOnStep {
        executionBuilder.value.suppressionSpecifier = specifier
        return ExecutionOnStepImpl(this, executionBuilder.value)
    }
}

class ExecutionOnStepImpl(
    private val vlExecutionInitStep: VlExecutionInitStep,
    private val executionBuilder: ExecutionBuilder,
) : ExecutionOnStep {
    override infix fun on(releaseGroup: ReleaseGroup): VlExecutionInitStep {
        val duration: String = getDuration(executionBuilder.suppressionSpecifier)
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        executionBuilder.executions.flatMap { it.releases }.contains(a)
                    }
            }
        executionBuilder.executions += Execution("suppress", duration, releaseList)
        return vlExecutionInitStep
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep {
        val releaseList = allReleases().filter { it in releases }
        val duration: String = getDuration(executionBuilder.suppressionSpecifier)
        executionBuilder.executions += Execution("suppress", duration, releaseList)
        return vlExecutionInitStep
    }

    override infix fun on(release: ReleaseBranch): VlExecutionInitStep {
        val duration: String = getDuration(executionBuilder.suppressionSpecifier)
        executionBuilder.executions += Execution("suppress", duration, listOf(release))
        return vlExecutionInitStep
    }

    private fun getDuration(relativePublication: SuppressionSpecifier?): String {
        return when (relativePublication) {
            is SuppressionSpecifierPermanent -> "permanent"
            is SuppressionSpecifierTemporarily -> "temporarily for ${relativePublication.duration}"
            is SuppressionSpecifierUntilNextPublication -> "until next release in release branch"
            null -> error("booom")
        }
    }
}

class VlExecutionSuppressTemporarilyStepImpl(
    private val vlExecutionInitStepImpl: VlExecutionInitStepImpl,
    private val executionBuilder: ExecutionBuilder,
) : VlExecutionSuppressTemporarilyStep {
    override infix fun forTime(duration: Duration): ExecutionOnStep {
        executionBuilder.suppressionSpecifier = SuppressionSpecifierTemporarily(duration)
        return ExecutionOnStepImpl(vlExecutionInitStepImpl, executionBuilder)
    }
}

data class VulnlogExecutionDataImpl(override val executions: List<VulnlogExecution>) : VulnlogExecutionData
