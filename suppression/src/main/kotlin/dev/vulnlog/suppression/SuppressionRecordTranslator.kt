package dev.vulnlog.suppression

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporter
import dev.vulnlog.dsl.VlReporterImpl
import java.time.format.DateTimeFormatter

class SuppressionRecordTranslator(private val tokenReplacer: SuppressionTokenReplacer) {
    private val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun translate(vulnsPerBranchAndRecord: Set<VulnsPerBranchAndRecord>): Set<SuppressionRecord> {
        return vulnsPerBranchAndRecord
            .asSequence()
            .flatMap { (releaseBranch, reporter, vuln) -> translateTemplates(reporter, vuln, releaseBranch) }
            .groupBy { it.reporter }
            .map { entry -> entry.key to collectSuppressionsPerReleaseBranch(entry) }
            .map { SuppressionRecord(it.first.config!!.templateFilename, it.second) }
            .toSet()
    }

    private fun translateTemplates(
        reporter: VlReporter,
        vuln: Set<SuppressVulnerability>,
        releaseBranch: ReleaseBranchData,
    ): Set<SuppressionEntry> {
        val vulnTemplate = (reporter as VlReporterImpl).config?.template!!
        return vuln.map { v ->
            val replaceMap =
                mapOf(
                    "vulnlogId" to v.id,
                    "vulnlogReasoning" to v.analysisReasoning,
                    "vulnlogStart" to v.suppressionStart.format(dateFormater),
                    "vulnlogEnd" to v.suppressionEnd?.format(dateFormater),
                )
            tokenReplacer.replaceTokens(SuppressionTokenData(vulnTemplate, replaceMap))
        }
            .map { SuppressionEntry(releaseBranch, reporter, it) }
            .toSet()
    }

    private fun collectSuppressionsPerReleaseBranch(entry: Map.Entry<VlReporterImpl, List<SuppressionEntry>>) =
        entry.value
            .filter { data -> data.reporter == entry.key }
            .groupBy({ it.branch }, { it.suppression })
}

private data class SuppressionEntry(
    val branch: ReleaseBranchData,
    val reporter: VlReporterImpl,
    val suppression: String,
)
