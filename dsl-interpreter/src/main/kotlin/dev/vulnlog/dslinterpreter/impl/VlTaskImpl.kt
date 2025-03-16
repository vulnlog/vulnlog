package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.All
import dev.vulnlog.dsl.AllOther
import dev.vulnlog.dsl.AnalysisData
import dev.vulnlog.dsl.ExecutionBuilder
import dev.vulnlog.dsl.NoActionAction
import dev.vulnlog.dsl.ReleaseBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseBranchProvider.Factory.allReleases
import dev.vulnlog.dsl.ReleaseGroup
import dev.vulnlog.dsl.Task
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.TaskData
import dev.vulnlog.dsl.UpdateAction
import dev.vulnlog.dsl.VlExecutionInitStep
import dev.vulnlog.dsl.VlTaskFollowUpSpecificationStep
import dev.vulnlog.dsl.VlTaskInitStep
import dev.vulnlog.dsl.VlTaskOnStep
import dev.vulnlog.dsl.VlTaskUpdateStep
import dev.vulnlog.dsl.VulnlogTaskData
import dev.vulnlog.dsl.WaitAction
import kotlin.time.Duration

class TaskBuilder(val analysisData: AnalysisData) {
    var dependencyName: String? = null
    var action: TaskAction? = null
    val tasks: MutableList<Task> = mutableListOf()

    fun build(): ExecutionBuilder {
        return ExecutionBuilder(TaskData(analysisData, tasks))
    }
}

class VlTaskInitStepImpl(private val taskBuilder: Lazy<TaskBuilder>) : VlTaskInitStep {
    override infix fun update(dependency: String): VlTaskUpdateStep {
        taskBuilder.value.dependencyName = dependency
        return VlTaskUpdateStepImpl(this, taskBuilder.value)
    }

    override infix fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        taskBuilder.value.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.value.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStepImpl(lazy { taskBuilder.value.build() })
    }

    override infix fun waitOnAllFor(duration: Duration): VlExecutionInitStep {
        taskBuilder.value.tasks += Task(WaitAction(duration), allReleases())
        return VlExecutionInitStepImpl(lazy { taskBuilder.value.build() })
    }
}

class VlTaskUpdateStepImpl(
    private val vlTaskInitStep: VlTaskInitStep,
    private val taskBuilder: TaskBuilder,
) : VlTaskUpdateStep {
    override infix fun atLeastTo(version: String): VlTaskOnStep {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return VlTaskOnStepImpl(vlTaskInitStep, taskBuilder)
    }
}

class VlTaskFollowUpSpecificationStepImpl(
    private val vlTaskInitStep: VlTaskInitStep,
    private val taskBuilder: TaskBuilder,
) : VlTaskFollowUpSpecificationStep {
    override infix fun andUpdateAtLeastTo(version: String): VlTaskOnStep {
        taskBuilder.action = UpdateAction(taskBuilder.dependencyName!!, version)
        return VlTaskOnStepImpl(vlTaskInitStep, taskBuilder)
    }

    override infix fun andNoActionOn(releases: ClosedRange<ReleaseBranch>): VlExecutionInitStep {
        val releaseList = allReleases().filter { it in releases }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStepImpl(lazy { taskBuilder.build() })
    }

    override infix fun andNoActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther -> allReleases().filterNot { a -> taskBuilder.tasks.flatMap { b -> b.releases }.contains(a) }
            }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStepImpl(lazy { taskBuilder.build() })
    }
}

class VlTaskOnStepImpl(
    private val vlTaskInitStep: VlTaskInitStep,
    private val taskBuilder: TaskBuilder,
) : VlTaskOnStep {
    override infix fun on(releaseGroup: ReleaseGroup): VlTaskFollowUpSpecificationStepImpl {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        taskBuilder.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return VlTaskFollowUpSpecificationStepImpl(vlTaskInitStep, taskBuilder)
    }

    override infix fun on(releases: ClosedRange<ReleaseBranch>): VlTaskFollowUpSpecificationStepImpl {
        val releaseList = allReleases().filter { it in releases }
        taskBuilder.tasks += Task(taskBuilder.action!!, releaseList)
        return VlTaskFollowUpSpecificationStepImpl(vlTaskInitStep, taskBuilder)
    }

    override infix fun on(release: ReleaseBranch): VlTaskFollowUpSpecificationStepImpl {
        taskBuilder.tasks += Task(taskBuilder.action!!, listOf(release))
        return VlTaskFollowUpSpecificationStepImpl(vlTaskInitStep, taskBuilder)
    }
}

data class VulnlogTaskDataImpl(override val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>) :
    VulnlogTaskData
