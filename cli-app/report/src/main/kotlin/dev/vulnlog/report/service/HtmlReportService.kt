package dev.vulnlog.report.service

import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.ReportBy
import dev.vulnlog.common.model.VulnEntry
import dev.vulnlog.report.HtmlReport
import java.io.File
import java.time.LocalDateTime

data class HtmlReportArguments(val reportOutputDir: File)

data class VulnEntryByReporter(val vulnerability: VulnEntry, val reporters: List<ReportBy>)

class HtmlReportService(
    private val serializerService: JsonSerializerService,
    private val htmlReportGeneratorService: HtmlReportGeneratorService,
    private val htmlReportFileWriterService: HtmlReportFileWriterService,
) {
    fun generateReport(
        htmlReportArguments: HtmlReportArguments,
        cliVersion: String,
        vulnEntriesFiltered: List<VulnEntry>,
    ) {
        val releaseBranchToVulnEntryByReporter: Map<BranchName, List<VulnEntryByReporter>> =
            vulnEntriesFiltered
                .filter { it.primaryVulnId }
                .groupBy { it.reportedFor.branchName }
                .map { (releaseBranchName, vulnEntries) ->
                    val vulnEntryByReports =
                        vulnEntries
                            .groupBy { it.id }
                            .map { (_, vulnEntries) ->
                                val vulnEntry: VulnEntry = vulnEntries.first()
                                val reporters: List<ReportBy> = vulnEntries.map { c -> c.reportedBy }
                                VulnEntryByReporter(vulnEntry, reporters)
                            }
                    releaseBranchName to vulnEntryByReports
                }
                .toMap()

        val branchToJson: Map<BranchName, String> =
            releaseBranchToVulnEntryByReporter
                .map { (releaseBranch, vulnEntries) ->
                    releaseBranch to serializerService.serialize(releaseBranch, vulnEntries)
                }.toMap()

        val htmlReports: List<HtmlReport> =
            branchToJson.map { (branchName, json) ->
                htmlReportGeneratorService.generateHtmlReport(cliVersion, json, branchName, LocalDateTime.now())
            }

        htmlReports.forEach { report ->
            htmlReportFileWriterService.writeFile(htmlReportArguments.reportOutputDir, report)
        }
    }
}
