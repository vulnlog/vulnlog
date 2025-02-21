package dev.vulnlog.dsl

import dev.vulnlog.dsl.ReleaseBranch.Factory.allReleases
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface Publication

val nextPublication =
    object : Publication {
        override fun toString(): String {
            return "next publication"
        }
    }

val Int.days: Duration
    get() = this.toDuration(DurationUnit.DAYS)

class ExecutionData(val taskData: TaskData)

class ExecutionBuilder(val taskData: TaskData) {
    val executions: MutableList<Execution> = mutableListOf()
    var absolutDuration: Duration? = null
    var relativePublication: Publication? = null

    fun build(): ExecutionData {
        return ExecutionData(taskData)
    }
}

class ExecutionInit2(private val executionBuilder: Lazy<ExecutionBuilder>) {
    infix fun suppressOn(releases: ClosedRange<ReleaseBranch>): ExecutionNext {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.value.executions += Execution("suppress", "permanent", releaseList)
        return ExecutionNext(this, executionBuilder.value)
    }
}

class ExecutionNext(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun andSuppressFor(duration: Duration): ExecutionOnAbsolute {
        executionBuilder.absolutDuration = duration
        return ExecutionOnAbsolute(executionInit2, executionBuilder)
    }

    infix fun andSuppressUntil(publication: Publication): ExecutionOnRelative {
        executionBuilder.relativePublication = publication
        return ExecutionOnRelative(executionInit2, executionBuilder)
    }
}

class ExecutionOnAbsolute(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun on(release: ReleaseBranch): ExecutionNext {
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.absolutDuration.toString(),
                listOf(release),
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): ExecutionInit2 {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.executions += Execution("suppress", executionBuilder.absolutDuration.toString(), releaseList)
        return executionInit2
    }

    infix fun on(releaseGroup: ReleaseGroup): ExecutionData {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        executionBuilder.executions.flatMap { it.releases }.contains(a)
                    }
            }
        executionBuilder.executions += Execution("suppress", executionBuilder.absolutDuration.toString(), releaseList)
        return executionBuilder.build()
    }
}

class ExecutionOnRelative(private val executionInit2: ExecutionInit2, private val executionBuilder: ExecutionBuilder) {
    infix fun on(release: ReleaseBranch): ExecutionNext {
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                listOf(release),
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): ExecutionNext {
        val releaseList = allReleases.filter { it in releases }
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                releaseList,
            )
        return ExecutionNext(executionInit2, executionBuilder)
    }

    infix fun on(releaseGroup: ReleaseGroup): ExecutionData {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        executionBuilder.executions.flatMap { b -> b.releases }.contains(a)
                    }
            }
        executionBuilder.executions +=
            Execution(
                "suppress",
                executionBuilder.relativePublication!!.toString(),
                releaseList,
            )
        return executionBuilder.build()
    }
}

data class Execution(val action: String, val duration: String, val releases: List<ReleaseBranch>)

sealed interface VulnlogExecutionData {
    val tasks: List<TaskData2>
}

data class VulnlogExecutionDataImpl(override val tasks: List<TaskData2>) : VulnlogExecutionData

object VulnlogExecutionDataEmpty : VulnlogExecutionData {
    override val tasks: List<TaskData2> = emptyList()

    override fun toString(): String {
        return "VulnlogExecutionDataEmpty()"
    }
}
