package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.TaskDataPerBranch
import dev.vulnlog.common.model.VulnlogTaskData
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.TaskAction

class TaskSplitter {
    fun filterOnReleaseBranch(
        releaseBranch: ReleaseBranchData,
        taskData: VulnlogTaskData?,
    ): TaskDataPerBranch? {
        return taskData?.let { data ->
            val filteredOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>> =
                data.taskOnReleaseBranch.entries
                    .associate { it.key to it.value.filter { rb -> rb == releaseBranch } }
                    .filter { it.value.isNotEmpty() }
            if (filteredOnReleaseBranch.keys.size > 1) {
                error("Multiple task actions for the same release branch are currently not supported")
            } else if (filteredOnReleaseBranch.isEmpty() || filteredOnReleaseBranch.keys.isEmpty()) {
                null
            } else {
                TaskDataPerBranch(filteredOnReleaseBranch.keys.first())
            }
        }
    }
}
