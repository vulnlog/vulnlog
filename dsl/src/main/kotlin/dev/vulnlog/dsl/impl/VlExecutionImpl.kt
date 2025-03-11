package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.Execution
import dev.vulnlog.dsl.ExecutionBuilder
import dev.vulnlog.dsl.ExecutionOnStep
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranch.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.SuppressionSpecifier
import dev.vulnlog.dsl.SuppressionSpecifierPermanent
import dev.vulnlog.dsl.SuppressionSpecifierTemporarily
import dev.vulnlog.dsl.SuppressionSpecifierUntilNextPublication
import dev.vulnlog.dsl.VlExecutionInitStep
import dev.vulnlog.dsl.VlExecutionSuppressTemporarilyStep
import kotlin.time.Duration

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
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        executionBuilder.executions.flatMap { it.releases }.contains(a)
                    }
            }
        executionBuilder.executions += Execution("suppress", duration, releaseList)
        return vlExecutionInitStep
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep {
        val releaseList = allReleases.filter { it in releases }
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
