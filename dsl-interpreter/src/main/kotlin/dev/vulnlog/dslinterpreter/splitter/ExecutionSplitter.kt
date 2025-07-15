package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.ExecutionPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.model.VulnlogExecutionData
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VulnlogExecution
import dev.vulnlog.dsl.VulnlogFixExecution
import dev.vulnlog.dsl.VulnlogSuppressPermanentExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilExecution
import dev.vulnlog.dsl.VulnlogSuppressUntilNextPublicationExecution

class ExecutionSplitter {
    fun filterOnReleaseBranch(
        releaseBranch: ReleaseBranchData,
        executionData: VulnlogExecutionData?,
    ): ExecutionDataPerBranch? =
        executionData?.let {
            val filteredOnReleaseBranch =
                executionData.executions
                    .map { a -> a.releases.filter { rb -> rb == releaseBranch }.associateBy { a } }
                    .flatMap { it.entries }
                    .groupBy { it.key }
                    .mapValues { entry -> entry.value.map { it.value }.first() }
                    .map(::createVulnlogExecution)
            return if (filteredOnReleaseBranch.isEmpty()) {
                null
            } else if (filteredOnReleaseBranch.size > 1) {
                error("Multiple execution actions for the same release branch are currently not supported")
            } else {
                ExecutionDataPerBranch(filteredOnReleaseBranch.first())
            }
        }

    private fun createVulnlogExecution(entry: Map.Entry<VulnlogExecution, ReleaseBranchData>): ExecutionPerBranch =
        when (val key: VulnlogExecution = entry.key) {
            is VulnlogFixExecution -> FixedExecutionPerBranch(fixDate = key.fixDate)
            is VulnlogSuppressPermanentExecution -> SuppressionPermanentExecutionPerBranch
            is VulnlogSuppressUntilExecution -> SuppressionDateExecutionPerBranch(suppressUntilDate = key.untilDate)
            is VulnlogSuppressUntilNextPublicationExecution -> SuppressionEventExecutionPerBranch
        }
}
