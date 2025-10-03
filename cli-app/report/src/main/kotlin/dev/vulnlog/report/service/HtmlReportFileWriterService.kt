package dev.vulnlog.report.service

import dev.vulnlog.dsl.util.toCamelCase
import dev.vulnlog.report.HtmlReport
import java.io.File

class HtmlReportFileWriterService {
    fun writeFile(
        outputDirectory: File,
        report: HtmlReport,
    ) {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val fileName = "report-${report.releaseBranchName.toCamelCase()}.html"
        val file = outputDirectory.resolve(fileName)
        file.bufferedWriter().use { it.write(report.htmlContent) }
    }
}
