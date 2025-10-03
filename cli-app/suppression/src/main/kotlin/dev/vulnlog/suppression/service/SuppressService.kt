package dev.vulnlog.suppression.service

import dev.vulnlog.common.SubcommandData
import dev.vulnlog.common.SuppressionExecution
import dev.vulnlog.suppression.MapperInput
import dev.vulnlog.suppression.OutputWriter
import dev.vulnlog.suppression.SuppressVulnerability
import dev.vulnlog.suppression.SuppressionFileInfo
import dev.vulnlog.suppression.SuppressionFilter
import dev.vulnlog.suppression.SuppressionRecord
import dev.vulnlog.suppression.SuppressionRecordTranslator
import dev.vulnlog.suppression.SuppressionVulnerabilityMapperService
import dev.vulnlog.suppression.SuppressionWriter
import dev.vulnlog.suppression.VulnsPerBranchAndRecord
import java.io.File

data class SuppressCommandArguments(val templateDir: File)

class SuppressService(
    private val config: SuppressCommandArguments,
    private val outputWriter: OutputWriter,
    private val data: SubcommandData,
    private val suppressionVulnerabilityMapperService: SuppressionVulnerabilityMapperService,
    private val suppressionFilter: SuppressionFilter,
    private val suppressionTranslator: SuppressionRecordTranslator,
) {
    fun generateSuppression() {
        val mapperInput: List<MapperInput> =
            data.vulnEntriesFiltered
                .groupBy { it.reportedFor.branchName }
                .map { (releaseBranch, entries) ->
                    val reporterToVulns: Map<String, List<SuppressVulnerability>> =
                        entries
                            .groupBy { it.reportedBy.reporterName }
                            .map { (reporter, entries2) ->
                                val suppressVulnerabilities =
                                    entries2
                                        .filter { it.execution?.execution is SuppressionExecution }
                                        .map { entry ->
                                            val suppressionExecution: SuppressionExecution =
                                                entry.execution?.execution!! as SuppressionExecution
                                            SuppressVulnerability(
                                                id = entry.id,
                                                status = entry.status,
                                                reporter = entry.reportedBy.reporterName,
                                                reportDate = entry.reportedBy.awareAt,
                                                analysisReasoning = entry.analysis?.reasoning!!,
                                                suppressType = suppressionExecution,
                                                suppressionStart = entry.reportedBy.awareAt,
                                                suppressionEnd = suppressionExecution.suppressUntilDate,
                                            )
                                        }
                                reporter to suppressVulnerabilities
                            }.toMap()
                    MapperInput(releaseBranch, reporterToVulns)
                }

        val vulnsToSuppress: Set<VulnsPerBranchAndRecord> =
            suppressionVulnerabilityMapperService.mapToRelevantVulnerabilities(mapperInput)

        val templateNameToContent: Map<SuppressionFileInfo, List<String>> =
            config.templateDir.listFiles()
                ?.filter { it.isFile }
                ?.associate { SuppressionFileInfo(it.nameWithoutExtension, it.extension) to it.readLines() }
                ?: emptyMap()
        if (templateNameToContent.isEmpty()) {
            error("No template files were found in: ${config.templateDir}")
        }
        val filteredVulnsToSuppress: Set<VulnsPerBranchAndRecord> = suppressionFilter.filter(vulnsToSuppress)
        val suppressionRecord: Set<SuppressionRecord> = suppressionTranslator.translate(filteredVulnsToSuppress)

        val suppressionWriter = SuppressionWriter(outputWriter, data.releaseBranchesFiltered)
        suppressionWriter.writeSuppression(templateNameToContent, suppressionRecord)
    }
}
