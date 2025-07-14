package dev.vulnlog.common.model

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.TaskAction

data class VulnlogTaskData(val taskOnReleaseBranch: Map<TaskAction, List<ReleaseBranchData>>)
