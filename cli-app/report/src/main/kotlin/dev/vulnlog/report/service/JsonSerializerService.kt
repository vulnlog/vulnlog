@file:Suppress("TooManyFunctions")

package dev.vulnlog.report.service

import dev.vulnlog.common.ExecutionDataPerBranch
import dev.vulnlog.common.FixedExecutionPerBranch
import dev.vulnlog.common.InvolvedRelease
import dev.vulnlog.common.SuppressionDateExecutionPerBranch
import dev.vulnlog.common.SuppressionEventExecutionPerBranch
import dev.vulnlog.common.SuppressionPermanentExecutionPerBranch
import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.VulnId
import dev.vulnlog.common.model.VulnStatus
import dev.vulnlog.common.model.VulnStatusAffected
import dev.vulnlog.common.model.VulnStatusFixed
import dev.vulnlog.common.model.VulnStatusNotAffected
import dev.vulnlog.common.model.VulnStatusUnderInvestigation
import dev.vulnlog.common.model.VulnStatusUnknown
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.dsl.NoActionAction
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.TaskAction
import dev.vulnlog.dsl.UpdateAction
import dev.vulnlog.dsl.WaitAction
import dev.vulnlog.report.serialisable.Analysis
import dev.vulnlog.report.serialisable.Execution
import dev.vulnlog.report.serialisable.FixExecution
import dev.vulnlog.report.serialisable.InvolvedReleaseVersion
import dev.vulnlog.report.serialisable.InvolvedReleaseVersions
import dev.vulnlog.report.serialisable.PermanentSuppressionExecution
import dev.vulnlog.report.serialisable.ReleaseBranche
import dev.vulnlog.report.serialisable.ReleaseVersion
import dev.vulnlog.report.serialisable.Report
import dev.vulnlog.report.serialisable.Task
import dev.vulnlog.report.serialisable.TemporarySuppressionExecution
import dev.vulnlog.report.serialisable.UntilNextReleaseSuppressionExecution
import dev.vulnlog.report.serialisable.VulnIdentifier
import dev.vulnlog.report.serialisable.Vulnerability
import dev.vulnlog.report.serialisable.Vulnlog
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

class JsonSerializerService(
    private val releaseBranchRepository: BranchRepository,
) {
    private val jsonFormatConfig =
        Json {
            allowStructuredMapKeys = true
            prettyPrint = true
            serializersModule =
                SerializersModule {
                    polymorphic(
                        Execution::class,
                        FixExecution::class,
                        FixExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        PermanentSuppressionExecution::class,
                        PermanentSuppressionExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        TemporarySuppressionExecution::class,
                        TemporarySuppressionExecution.serializer(),
                    )
                    polymorphic(
                        Execution::class,
                        UntilNextReleaseSuppressionExecution::class,
                        UntilNextReleaseSuppressionExecution.serializer(),
                    )
                }
        }

    fun serialize(
        releaseBranchesFiltered: BranchName,
        vulnEntriesFiltered: List<VulnEntryByReporter>,
    ): String {
        val releaseBranches: List<ReleaseBranche> = mapTo(releaseBranchesFiltered)
        val releaseBranchesVulnerabilities: List<Vulnerability> = mapTo(vulnEntriesFiltered)
        return jsonFormatConfig.encodeToString(Vulnlog(releaseBranches, releaseBranchesVulnerabilities))
    }

    private fun mapTo(branchName: BranchName): List<ReleaseBranche> {
        return releaseBranchRepository.getBranchesToReleaseVersions()
            .filter { releaseBranch -> BranchName(releaseBranch.key.name) == branchName }
            .map { (releaseBranch, releaseVersions) ->
                ReleaseBranche(releaseBranch.name, releaseVersions.map { it.toReleaseVersion() })
            }
    }

    private fun ReleaseVersionData.toReleaseVersion() =
        ReleaseVersion(
            version = version,
            publicationDate = releaseDate,
        )

    private fun mapTo(vulnEntryByReporter: List<VulnEntryByReporter>): List<Vulnerability> {
        return vulnEntryByReporter.map { entry ->
            val vulnEntry = entry.vulnerability
            Vulnerability(
                id = VulnIdentifier(vulnEntry.id.identifier, vulnEntry.groupIds.map(VulnId::identifier)),
                status = transformStatusToHumanReadableStatus(vulnEntry.status),
                reporters = entry.reporters.map { reporter -> Report(reporter.reporterName, reporter.awareAt) },
                analysis = vulnEntry.analysis?.let { Analysis(it.analysedAt, it.verdict.level, it.reasoning) },
                task = vulnEntry.task?.let { transformTaskActionTuHumanReadableTask(it.taskAction) },
                execution =
                    vulnEntry.execution?.let {
                        translateExecution(
                            it,
                            vulnEntry.reportedFor.branchName,
                            vulnEntry.involved,
                        )
                    },
                involved =
                    vulnEntry.involved?.let {
                        InvolvedReleaseVersions(
                            InvolvedReleaseVersion(it.affected?.version, it.affected?.releaseDate),
                            InvolvedReleaseVersion(it.upcoming?.version, it.upcoming?.releaseDate),
                        )
                    },
            )
        }
    }

    private fun transformStatusToHumanReadableStatus(status: VulnStatus): String =
        when (status) {
            VulnStatusAffected -> "affected"
            VulnStatusFixed -> "fixed"
            VulnStatusNotAffected -> "not affected"
            VulnStatusUnderInvestigation -> "under investigation"
            VulnStatusUnknown -> "unknown"
        }

    private fun transformTaskActionTuHumanReadableTask(task: TaskAction): Task {
        return when (task) {
            is NoActionAction -> Task("no action required")
            is UpdateAction -> Task("update", listOf(task.dependency, "to", task.version))
            is WaitAction -> Task("wait", listOf("for", task.forAmountOfTime.inWholeDays.toString(), "days"))
        }
    }

    private fun translateExecution(
        executionData: ExecutionDataPerBranch,
        branchName: BranchName,
        involved: InvolvedRelease?,
    ): Execution {
        val execution = executionData.execution
        val action = execution.action
        return when (execution) {
            is FixedExecutionPerBranch -> FixExecution(action, branchName.name, execution.fixDate)
            is SuppressionDateExecutionPerBranch ->
                TemporarySuppressionExecution(
                    action,
                    branchName.name,
                    execution.suppressUntilDate,
                )

            is SuppressionEventExecutionPerBranch ->
                UntilNextReleaseSuppressionExecution(
                    action,
                    branchName.name,
                    involved?.upcoming?.version,
                    involved?.upcoming?.releaseDate,
                )

            is SuppressionPermanentExecutionPerBranch -> PermanentSuppressionExecution(action, branchName.name)
        }
    }
}
