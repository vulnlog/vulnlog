package dev.vulnlog.report

import java.io.File

data class HtmlReport(
    val releaseBranchName: String,
    val htmlContent: String,
) {
    fun writeFile(output: File) {
        val file = output.resolve("report-$releaseBranchName.html")
        file.bufferedWriter().use { it.write(htmlContent) }
    }
}
