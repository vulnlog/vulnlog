package dev.vulnlog.suppression

import dev.vulnlog.dsl.VlReporterImpl
import java.time.format.DateTimeFormatter

class SuppressionRecordTranslator(private val tokenReplacer: SuppressionTokenReplacer) {
    private val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun translate(vulnsPerBranchAndRecord: Set<VulnsPerBranchAndRecord>): Set<SuppressionRecord> {
        return vulnsPerBranchAndRecord.flatMap { (releaseBranch, reporter, vuln) ->
            val vulnTemplate = (reporter as VlReporterImpl).config?.template!!
            vuln.map { v ->
                val replaceMap =
                    mapOf(
                        "vulnlogId" to v.id,
                        "vulnlogReasoning" to v.analysisReasoning,
                        "vulnlogStart" to v.suppressionStart.format(dateFormater),
                        "vulnlogEnd" to v.suppressionEnd?.format(dateFormater),
                    )
                tokenReplacer.replaceTokens(SuppressionTokenData(vulnTemplate, replaceMap))
            }
                .map { SuppressionRecord(releaseBranch, reporter, it) }
                .toSet()
        }.toSet()
    }
}
