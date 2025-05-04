@file:Suppress("TooManyFunctions")

package dev.vulnlog.cli.commands

import Filtered
import dev.vulnlog.cli.serialisable.Analysis
import dev.vulnlog.cli.serialisable.Execution
import dev.vulnlog.cli.serialisable.FixExecution
import dev.vulnlog.cli.serialisable.InvolvedReleaseVersion
import dev.vulnlog.cli.serialisable.InvolvedReleaseVersions
import dev.vulnlog.cli.serialisable.PermanentSuppressionExecution
import dev.vulnlog.cli.serialisable.ReleaseBranchVulnerabilities
import dev.vulnlog.cli.serialisable.ReleaseBranche
import dev.vulnlog.cli.serialisable.ReleaseVersion
import dev.vulnlog.cli.serialisable.Report
import dev.vulnlog.cli.serialisable.Task
import dev.vulnlog.cli.serialisable.TemporarySuppressionExecution
import dev.vulnlog.cli.serialisable.UntilNextReleaseSuppressionExecution
import dev.vulnlog.cli.serialisable.Vulnerability
import dev.vulnlog.cli.serialisable.Vulnlog
import dev.vulnlog.common.AnalysisDataPerBranch
import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.ReportDataPerBranch
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.TaskDataPerBranch
import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.NoActionAction
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.UpdateAction
import dev.vulnlog.dsl.WaitAction

class SerialisationTranslator {
    fun translate(filteredResult: Filtered): Vulnlog {
        val releaseBranches = filteredResult.releaseBranches.toReleaseBranches()
        val releaseBrancheVulnerabilities = filteredResult.vulnerabilitiesPerBranch.toReleaseBranchVulnerabilities()
        return Vulnlog(releaseBranches, releaseBrancheVulnerabilities)
    }

    private fun Map<ReleaseBranchData, List<ReleaseVersionData>>.toReleaseBranches(): List<ReleaseBranche> {
        return map { (releaseBranch, releaseVersions) ->
            ReleaseBranche(releaseBranch.name, releaseVersions.map { it.toReleaseVersion() })
        }
    }

    private fun ReleaseVersionData.toReleaseVersion() =
        ReleaseVersion(
            version = version,
            publicationDate = releaseDate,
        )

    private fun Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>.toReleaseBranchVulnerabilities() =
        map { (releaseBranch, vulnerabilities) ->
            ReleaseBranchVulnerabilities(releaseBranch.name, vulnerabilities.toVulnerability())
        }

    private fun List<VulnerabilityDataPerBranch>.toVulnerability(): List<Vulnerability> {
        return map { vulnlogData ->
            Vulnerability(
                vulnlogData.ids,
                vulnlogData.status,
                vulnlogData.reportData.toReport(),
                vulnlogData.analysisData?.toAnalysis(),
                vulnlogData.taskData?.toTask(),
                vulnlogData.executionData?.toExecution(vulnlogData.branch, vulnlogData.involvedReleaseVersions),
                vulnlogData.involvedReleaseVersions?.toInvolvedReleaseVersions(),
            )
        }
    }

    private fun ReportDataPerBranch.toReport(): Set<Report> {
        return reporters.map { Report(it.name, awareAt) }.toSet()
    }

    private fun AnalysisDataPerBranch.toAnalysis(): Analysis {
        return Analysis(analysedAt, verdict.level, reasoning)
    }

    private fun TaskDataPerBranch.toTask(): Task? {
        return when (val task = taskAction) {
            is NoActionAction -> Task("no action required")
            is UpdateAction -> Task("update", listOf(task.dependency, "to", task.version))
            is WaitAction -> Task("wait", listOf("for", task.forAmountOfTime.inWholeDays.toString(), "days"))
        }
    }

    private fun ExecutionDataPerBranch.toExecution(
        branch: ReleaseBranchData,
        involved: dev.vulnlog.dsl.InvolvedReleaseVersion?,
    ): Execution? {
        val execution = execution
        val action = execution.action
        return when (execution) {
            is FixedExecutionPerBranch -> FixExecution(action, branch.name, execution.fixDate)
            is SuppressionDateExecutionPerBranch ->
                TemporarySuppressionExecution(
                    action,
                    branch.name,
                    execution.suppressUntilDate,
                )

            is SuppressionEventExecutionPerBranch ->
                UntilNextReleaseSuppressionExecution(
                    action,
                    branch.name,
                    involved?.upcoming?.version,
                    involved?.upcoming?.releaseDate,
                )

            is SuppressionPermanentExecutionPerBranch -> PermanentSuppressionExecution(action, branch.name)
        }
    }

    private fun dev.vulnlog.dsl.InvolvedReleaseVersion.toInvolvedReleaseVersions(): InvolvedReleaseVersions? {
        val involvedAffected = involvedReleaseVersion(affected, affected)
        val involvedUpcoming = involvedReleaseVersion(upcoming, upcoming)
        return if (involvedAffected == null && involvedUpcoming == null) {
            null
        } else {
            InvolvedReleaseVersions(involvedAffected, involvedUpcoming)
        }
    }

    private fun involvedReleaseVersion(
        involvedAffected: ReleaseVersionData?,
        involvedUpcoming: ReleaseVersionData?,
    ): InvolvedReleaseVersion? =
        if (involvedAffected == null && involvedUpcoming == null) {
            null
        } else {
            InvolvedReleaseVersion(involvedAffected?.version, involvedUpcoming?.releaseDate)
        }
}
