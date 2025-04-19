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
import dev.vulnlog.dsl.VlExecutionInitStep
import dev.vulnlog.dsl.VlTaskFollowUpSpecificationStep
import dev.vulnlog.dsl.VlTaskInitStep
import dev.vulnlog.dsl.VlTaskOnStep
import dev.vulnlog.dsl.VlTaskUpdateStep
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
    override fun update(dependency: String): VlTaskUpdateStep {
        taskBuilder.dependencyName = dependency
        return VlTaskUpdateStepImpl(this, taskBuilder)
    }

    override fun noActionOn(releaseGroup: ReleaseGroup): VlExecutionInitStep {
        val releaseList: List<ReleaseBranch> =
            when (releaseGroup) {
                All -> allReleases()
                AllOther ->
                    allReleases().filterNot { a ->
                        taskBuilder.tasks.flatMap { b -> b.releases }.contains(a)
                    }
            }
        taskBuilder.tasks += Task(NoActionAction, releaseList)
        return VlExecutionInitStepImpl(lazy { taskBuilder.build() })
    }

    override fun waitOnAllFor(duration: Duration): VlExecutionInitStep {
        taskBuilder.tasks += Task(WaitAction(duration), allReleases())
        return VlExecutionInitStepImpl(lazy { taskBuilder.build() })
    }

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
