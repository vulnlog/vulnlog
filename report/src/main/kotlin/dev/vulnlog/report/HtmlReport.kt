package dev.vulnlog.report

import dev.vulnlog.dsl.util.toCamelCase
import java.io.File

data class HtmlReport(
    val releaseBranchName: String,
    val htmlContent: String,
) {
    fun writeFile(output: File) {
        val fileName = "report-${releaseBranchName.toCamelCase()}.html"
        val file = output.resolve(fileName)
        file.bufferedWriter().use { it.write(htmlContent) }
    }
}
