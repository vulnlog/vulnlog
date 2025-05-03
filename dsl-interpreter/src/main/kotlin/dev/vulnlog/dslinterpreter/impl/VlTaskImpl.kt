package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.NoActionAction
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.UpdateAction
import dev.vulnlog.dsl.VlExecutionInitState
import dev.vulnlog.dsl.VlTaskFollowState
import dev.vulnlog.dsl.VlTaskInitState
import dev.vulnlog.dsl.VlTaskOnState
import dev.vulnlog.dsl.VlTaskUpdateState
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dsl.WaitAction
import kotlin.time.Duration

data class DslTaskData(val analysisData: DslAnalysisData?, val tasks: List<Task>)

data class Task(val taskAction: TaskAction, val releases: List<ReleaseBranch>)

class TaskBuilder(val dslAnalysisData: DslAnalysisData) {
    var dependencyName: String? = null
    var action: TaskAction? = null
    val tasks: MutableList<Task> = mutableListOf()

    fun build(): ExecutionBuilder {
        return ExecutionBuilder(DslTaskData(dslAnalysisData, tasks))
    }
}

class VlTaskInitStateImpl(private val taskBuilder: Lazy<TaskBuilder>) : VlTaskInitState {
    override infix fun update(dependency: String): VlTaskUpdateState {
        taskBuilder.value.dependencyName = dependency
        return VlTaskUpdateStateImpl(this, taskBuilder.value)
    }

    override infix fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitState {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        taskBuilder.value.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.value.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStateImpl(lazy { taskBuilder.value.build() })
    }

    override infix fun waitOnAllFor(duration: Duration): VlExecutionInitState {
        taskBuilder.value.tasks += Task(WaitAction(duration), allReleases())
        return VlExecutionInitStateImpl(lazy { taskBuilder.value.build() })
    }
}

class VlTaskUpdateStateImpl(
    private val vlTaskInitState: VlTaskInitState,
    private val taskBuilder: TaskBuilder,
) : VlTaskUpdateState {
    override infix fun atLeastTo(version: String): VlTaskOnState {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return VlTaskOnStateImpl(vlTaskInitState, taskBuilder)
    }
}

class VlTaskFollowStateImpl(
    private val vlTaskInitState: VlTaskInitState,
    private val taskBuilder: TaskBuilder,
) : VlTaskFollowState {
    override infix fun andUpdateAtLeastTo(version: String): VlTaskOnState {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return VlTaskOnStateImpl(vlTaskInitState, taskBuilder)
    }

    override infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): VlExecutionInitState {
        val releaseList = allReleases().filter { it in releases }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStateImpl(lazy { taskBuilder.build() })
    }

    override infix fun andNoActionOn(releaseGroup: ReleaseGroup): VlExecutionInitState {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther -> allReleases().filterNot { a -> taskBuilder.tasks.flatMap { b -> b.releases }.contains(a) }
            }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStateImpl(lazy { taskBuilder.build() })
    }
}

class VlTaskOnStateImpl(
    private val vlTaskInitState: VlTaskInitState,
    private val taskBuilder: TaskBuilder,
) : VlTaskOnState {
    override infix fun on(releaseGroup: ReleaseGroup): VlTaskFollowStateImpl {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        taskBuilder.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return VlTaskFollowStateImpl(vlTaskInitState, taskBuilder)
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlTaskFollowStateImpl {
        val releaseList = allReleases().filter { it in releases }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return VlTaskFollowStateImpl(vlTaskInitState, taskBuilder)
    }

    override infix fun on(release: ReleaseBranch): VlTaskFollowStateImpl {
        taskBuilder.tasks += Task(taskBuilder.action!!, listOf(release))
        return VlTaskFollowStateImpl(vlTaskInitState, taskBuilder)
    }
}

data class VulnlogTaskDataImpl(override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>) :
    VulnlogTaskData
