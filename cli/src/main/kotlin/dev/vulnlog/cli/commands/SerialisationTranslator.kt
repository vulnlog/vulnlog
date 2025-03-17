package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.Analysis
import dev.vulnlog.cli.serialisable.Execution
import dev.vulnlog.cli.serialisable.ReleaseBranchVulnerabilities
import dev.vulnlog.cli.serialisable.ReleaseBranche
import dev.vulnlog.cli.serialisable.ReleaseVersion
import dev.vulnlog.cli.serialisable.Report
import dev.vulnlog.cli.serialisable.Task
import dev.vulnlog.cli.serialisable.Vulnerability
import dev.vulnlog.cli.serialisable.Vulnlog
import dev.vulnlog.dsl.NoActionAction
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.UpdateAction
import dev.vulnlog.dsl.VulnerabilityData
import dev.vulnlog.dsl.VulnlogAnalysisData
import dev.vulnlog.dsl.VulnlogExecutionData
import dev.vulnlog.dsl.VulnlogReportData
import dev.vulnlog.dsl.VulnlogTaskData
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

    private fun Map<ReleaseBranchData, List<VulnerabilityData>>.toReleaseBranchVulnerabilities() =
        map { (releaseBranch, vulnerabilities) ->
            ReleaseBranchVulnerabilities(releaseBranch.name, vulnerabilities.toVulnerability())
        }

    private fun List<VulnerabilityData>.toVulnerability(): List<Vulnerability> {
        return map { vulnlogData ->
            Vulnerability(
                vulnlogData.ids,
                vulnlogData.reportData?.toReport(),
                vulnlogData.analysisData?.toAnalysis(),
                vulnlogData.taskData?.toTask(),
                vulnlogData.executionData?.toExecution(),
            )
        }
    }

    private fun VulnlogReportData.toReport(): Report {
        return Report(reporter.name, awareAt)
    }

    private fun VulnlogAnalysisData.toAnalysis(): Analysis {
        return Analysis(analysedAt, verdict.level, reasoning)
    }

    private fun VulnlogTaskData.toTask(): Task? {
        if (taskOnReleaseBranch.keys.isEmpty()) {
            return null
        }
        return when (val task = taskOnReleaseBranch.keys.first()) {
            is NoActionAction -> Task("no action required")
            is UpdateAction -> Task("update", listOf(task.dependency, "to", task.version))
            is WaitAction -> Task("wait", listOf("for", task.forAmountOfTime.inWholeDays.toString(), "days"))
        }
    }

    private fun VulnlogExecutionData.toExecution(): Execution? {
        if (executions.isEmpty()) {
            return null
        }
        val execution = executions.first()
        return Execution(execution.action, execution.duration)
    }
}
