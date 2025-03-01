package dev.vulnlog.dsl

import dev.vulnlog.dsl.ReleaseBranch.Factory.allReleases
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

sealed interface SuppressionSpecifier

data object SuppressionSpecifierPermanent : SuppressionSpecifier

val permanent = SuppressionSpecifierPermanent

data class SuppressionSpecifierTemporarily(val duration: Duration) : SuppressionSpecifier {
    companion object {
        val temporarily = SuppressionSpecifierTemporarily(0.days)
    }
}

data object SuppressionSpecifierUntilNextPublication : SuppressionSpecifier

val untilNextPublication = SuppressionSpecifierUntilNextPublication

class SuppressionTemporarily(private val executionInit: ExecutionInit, private val executionBuilder: ExecutionBuilder) {
    infix fun forTime(duration: Duration): ExecutionOn {
        executionBuilder.suppressionSpecifier = SuppressionSpecifierTemporarily(duration)
        return ExecutionOn(executionInit, executionBuilder)
    }
}

class ExecutionInit(private val executionBuilder: Lazy<ExecutionBuilder>) {
    infix fun suppress(specifier: SuppressionSpecifierPermanent): ExecutionOn {
        executionBuilder.value.suppressionSpecifier = specifier
        return ExecutionOn(this, executionBuilder.value)
    }

    infix fun suppress(specifier: SuppressionSpecifierTemporarily): SuppressionTemporarily {
        executionBuilder.value.suppressionSpecifier = specifier
        return SuppressionTemporarily(this, executionBuilder.value)
    }

    infix fun suppress(specifier: SuppressionSpecifierUntilNextPublication): ExecutionOn {
        executionBuilder.value.suppressionSpecifier = specifier
        return ExecutionOn(this, executionBuilder.value)
    }
}

class ExecutionOn(private val executionInit: ExecutionInit, private val executionBuilder: ExecutionBuilder) {
    infix fun on(release: ReleaseBranch): ExecutionInit {
        val duration: String = getDuration(executionBuilder.suppressionSpecifier)
        executionBuilder.executions += Execution("suppress", duration, listOf(release))
        return executionInit
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): ExecutionInit {
        val releaseList = allReleases.filter { it in releases }
        val duration: String = getDuration(executionBuilder.suppressionSpecifier)
        executionBuilder.executions += Execution("suppress", duration, releaseList)
        return executionInit
    }

    infix fun on(releaseGroup: ReleaseGroup): ExecutionInit {
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
        return executionInit
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
