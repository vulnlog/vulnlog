package dev.vulnlog.dsl2

import java.time.LocalDate

typealias Task = () -> VlResolutionTask

sealed interface TaskAction

data class TaskActionFixInRelease(val releaseBranchName: String, val release: MyRelease) : TaskAction

data class TaskActionSuppress(val suppress: Suppress) : TaskAction

data class TaskActionIgnore(val forAmountOfTime: TaskTimeUnit) : TaskAction

infix fun Task.fixIn(whenToFix: TaskPointInTime): TaskPlanContinuation {
    val resolution: VlResolutionTask = invoke()
    val reportedAt: LocalDate = resolution.resolutionTask.rating.reportFor.at
    val upcomingRelease: MutableList<TaskAction> =
        when (whenToFix) {
            NextRelease ->
                resolution.releaseBranches
                    .map { it to it.releaseJustAfter(reportedAt) }
                    .map { TaskActionFixInRelease(it.first.name, it.second) }
                    .toMutableList()
        }
    return TaskPlanContinuation(resolution, upcomingRelease)
}

infix fun Task.suppress(suppress: Suppress): TaskPlan {
    return TaskPlan(invoke(), mutableListOf(TaskActionSuppress(suppress)))
}

infix fun Task.waitAndReviewInWeeks(amount: Int): TaskPlan {
    return TaskPlan(invoke(), mutableListOf(TaskActionIgnore(Weeks(amount))))
}

sealed interface TaskTimeUnit {
    val amount: Int
}

data class Weeks(override val amount: Int) : TaskTimeUnit

sealed interface TaskPointInTime

data object NextRelease : TaskPointInTime

sealed interface Suppress

data object Temporarily : Suppress

data object Permanently : Suppress

interface MyTaskPlan {
    val resolution: VlResolutionTask
    val taskAction: MutableList<TaskAction>
}

class TaskPlan(
    override val resolution: VlResolutionTask,
    override val taskAction: MutableList<TaskAction>,
) : MyTaskPlan

class TaskPlanContinuation(
    override val resolution: VlResolutionTask,
    override val taskAction: MutableList<TaskAction>,
) : MyTaskPlan {
    infix fun andSuppress(suppress: Suppress): MyTaskPlan {
        taskAction.add(TaskActionSuppress(suppress))
        return this
    }
}
