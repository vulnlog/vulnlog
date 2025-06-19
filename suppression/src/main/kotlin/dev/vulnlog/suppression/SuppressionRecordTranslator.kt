package dev.vulnlog.suppression

import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.ReporterConfig
import dev.vulnlog.common.repository.ReporterRepository
import java.time.format.DateTimeFormatter

class SuppressionRecordTranslator(
    private val reporterRepository: ReporterRepository,
    private val tokenReplacer: SuppressionTokenReplacer,
) {
    private val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun translate(vulnsPerBranchAndRecord: Set<VulnsPerBranchAndRecord>): Set<SuppressionRecord> {
        return vulnsPerBranchAndRecord
            .asSequence()
            .flatMap { (releaseBranch, reporter, vuln) -> translateTemplates(reporter, vuln, releaseBranch) }
            .groupBy { it.reporter }
            .map { entry -> entry.key to collectSuppressionsPerReleaseBranch(entry) }
            .map { SuppressionRecord(getReporterConfig(it.first).templateFilename, it.second) }
            .toSet()
    }

    private fun getReporterConfig(reporter: String): ReporterConfig {
        return reporterRepository.getReportersWithConfig().first { it.reporterName == reporter }
    }

    private fun translateTemplates(
        reporter: String,
        vuln: Set<SuppressVulnerability>,
        releaseBranch: BranchName,
    ): Set<SuppressionEntry> {
        val vulnTemplate = getReporterConfig(reporter).template
        if (vuln.isEmpty()) return setOf(SuppressionEntry(releaseBranch, reporter, ""))

        return vuln.map { v ->
            val replaceMap =
                mapOf(
                    "vulnlogId" to v.id.identifier,
                    "vulnlogReasoning" to v.analysisReasoning,
                    "vulnlogStart" to v.suppressionStart.format(dateFormater),
                    "vulnlogEnd" to v.suppressionEnd?.format(dateFormater),
                )
            tokenReplacer.replaceTokens(SuppressionTokenData(vulnTemplate, replaceMap))
        }
            .map { SuppressionEntry(releaseBranch, reporter, it) }
            .toSet()
    }

    private fun collectSuppressionsPerReleaseBranch(entry: Map.Entry<String, List<SuppressionEntry>>) =
        entry.value
            .filter { data -> data.reporter == entry.key }
            .groupBy({ it.branch }, { it.suppression })
}

private data class SuppressionEntry(
    val branch: BranchName,
    val reporter: String,
    val suppression: String,
)
