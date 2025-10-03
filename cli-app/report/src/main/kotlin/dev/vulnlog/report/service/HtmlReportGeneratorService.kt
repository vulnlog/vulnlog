package dev.vulnlog.report.service

import dev.vulnlog.common.model.BranchName
import dev.vulnlog.report.HtmlReport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HtmlReportGeneratorService {
    fun generateHtmlReport(
        cliVersion: String,
        jsonData: String,
        releaseBranchName: BranchName,
        creationData: LocalDateTime,
    ): HtmlReport {
        var htmlReport = createHtmlSkeleton()

        val formatedDate = creationData.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val reportJsonData =
            "{ \"releaseBranchName\": \"${releaseBranchName.name}\", \"generationDate\":\"$formatedDate\"}"
        val wrappedJsonReportData = "<script type=\"application/json\" id=\"report-data\">$reportJsonData</script>"
        val reportMarker = "    <!-- report-data -->"
        htmlReport = htmlReport.replace(reportMarker, wrappedJsonReportData)

        val wrappedJsonVulnData = "<script type=\"application/json\" id=\"vuln-data\">$jsonData</script>"
        val vulnMarker = "    <!-- vuln-data -->"
        htmlReport = htmlReport.replace(vulnMarker, wrappedJsonVulnData)

        val footerValue = "This report was generated with Vulnlog $cliVersion"
        val footerMarker = "        <!-- report-footer -->"
        htmlReport = htmlReport.replace(footerMarker, footerValue)

        return HtmlReport(releaseBranchName.name, htmlReport)
    }

    private fun createHtmlSkeleton(): String {
        val template = readAsString("/branch-template.html")
        val datatablesCss = readAsString("/datatables.min.css")
        val datatablesJs = readAsString("/datatables.min.js")
        val logo = readAsString("/logo-vulnlog.svg").minify()

        var report = template.replace("        /* datatables-css */", "        $datatablesCss")
        report = report.replace("        // datatables-js", "        $datatablesJs")
        report = report.replace("                    <!-- logo-vulnlog -->", "                    $logo")
        return report
    }

    private fun readAsString(filename: String) =
        object {}.javaClass.getResource(filename)?.readText(Charsets.UTF_8).orEmpty()

    private fun String.minify(): String = this.replace("\n", " ").replace("\\s+".toRegex(), " ")
}
