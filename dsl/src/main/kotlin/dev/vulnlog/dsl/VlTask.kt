package dev.vulnlog.dsl

import dev.vulnlog.dsl.ReleaseBranch.Factory.allReleases
import kotlin.time.Duration

sealed interface ReleaseGroup

data object All : ReleaseGroup

data object AllOther : ReleaseGroup

val all = All
val allOther = AllOther

data class TaskData(val analysisData: AnalysisData, val tasks: List<Task>)

class TaskBuilder(val analysisData: AnalysisData) {
    var dependencyName: String? = null
    var action: TaskAction? = null
    val tasks: MutableList<Task> = mutableListOf()

    fun build(): ExecutionBuilder {
        return ExecutionBuilder(TaskData(analysisData, tasks))
    }
}

class TaskInit(private val taskBuilder: Lazy<TaskBuilder>) {
    infix fun update(dependency: String): TaskUpdateInit {
        taskBuilder.value.dependencyName = dependency
        return TaskUpdateInit(this, taskBuilder.value)
    }

    infix fun waitOnAllFor(duration: Duration): ExecutionInit {
        taskBuilder.value.tasks += Task(WaitAction(duration), allReleases)
        return ExecutionInit(lazy { taskBuilder.value.build() })
    }

    infix fun noActionOn(releaseGroup: ReleaseGroup): ExecutionInit {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        taskBuilder.value.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.value.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit(lazy { taskBuilder.value.build() })
    }
}

class TaskUpdateInit(private val taskInit: TaskInit, private val taskBuilder: TaskBuilder) {
    infix fun atLeastTo(version: String): TaskOn {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return TaskOn(taskInit, taskBuilder)
    }
}

class TaskNext(private val taskInit: TaskInit, private val taskBuilder: TaskBuilder) {
    infix fun andUpdateAtLeastTo(version: String): TaskOn {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return TaskOn(taskInit, taskBuilder)
    }

    infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): ExecutionInit {
        val releaseList = allReleases.filter { it in releases }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit(lazy { taskBuilder.build() })
    }

    infix fun andNoActionOn(releaseGroup: ReleaseGroup): ExecutionInit {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther -> allReleases.filterNot { a -> taskBuilder.tasks.flatMap { b -> b.releases }.contains(a) }
            }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return ExecutionInit(lazy { taskBuilder.build() })
    }
}

class TaskOn(private val taskInit: TaskInit, private val taskBuilder: TaskBuilder) {
    infix fun on(release: ReleaseBranch): TaskNext {
        taskBuilder.tasks += Task(taskBuilder.action!!, listOf(release))
        return TaskNext(taskInit, taskBuilder)
    }

    infix fun on(releases: ClosedRange<ReleaseBranch>): TaskNext {
        val releaseList = allReleases.filter { it in releases }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return TaskNext(taskInit, taskBuilder)
    }

    infix fun on(releaseGroup: ReleaseGroup): TaskNext {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases
                AllOther ->
                    allReleases.filterNot { a ->
                        taskBuilder.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return TaskNext(taskInit, taskBuilder)
    }
}

sealed interface TaskAction

data object NoActionAction : TaskAction

data class UpdateAction(val dependency: String, val version: String) : TaskAction

data class WaitAction(val forAmountOfTime: Duration) : TaskAction

data class Task(val taskAction: TaskAction, val releases: List<ReleaseBranch>)

sealed interface VulnlogTaskData {
    val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>
}

data class VulnlogTaskDataImpl(override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>) :
    VulnlogTaskData

object VulnlogTaskDataEmpty : VulnlogTaskData {
    override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>> = emptyMap()

    override fun toString(): String {
        return "VulnlogAnalysisDataEmpty()"
    }
}

sealed interface TaskData2 {
    val taskAction: TaskAction
    val releases: List<ReleaseBranchData>
}

data class TaskDataImpl(override val taskAction: TaskAction, override val releases: List<ReleaseBranchData>) : TaskData2
