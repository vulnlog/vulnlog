package dev.vulnlog.suppression

import dev.vulnlog.dsl.VlReporterImpl
import java.time.format.DateTimeFormatter

class SuppressionRecordTranslator {
    private val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun translate(vulnsPerBranchAndRecord: Set<VulnsPerBranchAndRecord>): Set<SuppressionRecord> {
        return vulnsPerBranchAndRecord.flatMap { (releaseBranch, reporter, vuln) ->
            val vulnTemplate = (reporter as VlReporterImpl).config?.template!!
            vuln.map { v ->
                val suppression =
                    vulnTemplate
                        .replace("vulnlog-id", v.id)
                        .replace("vulnlog-reasoning", v.analysisReasoning)
                        .replace("vulnlog-start", v.suppressionStart.format(dateFormater))
                v.suppressionEnd?.let { suppression.replace("vulnlog-end", it.format(dateFormater)) } ?: suppression
            }
                .map { SuppressionRecord(releaseBranch, reporter, it) }
                .toSet()
        }.toSet()
    }
}
