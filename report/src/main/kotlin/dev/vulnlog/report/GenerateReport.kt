package dev.vulnlog.report

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun generateReport(
    cliVersion: String,
    jsonData: String,
    releaseBranchName: String,
    creationData: LocalDateTime,
): HtmlReport {
    var htmlReport = createHtmlSkeleton()

    val formatedDate = creationData.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val reportJsonData = "{ \"releaseBranchName\": \"$releaseBranchName\", \"generationDate\":\"$formatedDate\"}"
    val wrappedJsonReportData = "<script type=\"application/json\" id=\"report-data\">$reportJsonData</script>"
    val reportMarker = "    <!-- report-data -->"
    htmlReport = htmlReport.replace(reportMarker, wrappedJsonReportData)

    val wrappedJsonVulnData = "<script type=\"application/json\" id=\"vuln-data\">$jsonData</script>"
    val vulnMarker = "    <!-- vuln-data -->"
    htmlReport = htmlReport.replace(vulnMarker, wrappedJsonVulnData)

    val footerValue = "This report was generated with Vulnlog $cliVersion"
    val footerMarker = "        <!-- report-footer -->"
    htmlReport = htmlReport.replace(footerMarker, footerValue)

    return HtmlReport(releaseBranchName, htmlReport)
}

private fun createHtmlSkeleton(): String {
    val template = readAsString("/branch-template.html")
    val bulmaCss = readAsString("/bulma.css")
    val datatablesCss = readAsString("/datatables.css")
    val datatablesJs = readAsString("/datatables.js")
    val logo = readAsString("/logo-vulnlog.svg").replace("\n", " ").replace("\\s+".toRegex(), " ")

    var report = template.replace("        /* datatables-css */", datatablesCss)
    report = report.replace("        // datatables-js", datatablesJs)
    report = report.replace("        /* bulma-css */", bulmaCss)
    report = report.replace("                    <!-- logo-vulnlog -->", "                    $logo")
    return report
}

private fun readAsString(filename: String) =
    object {}.javaClass.getResource(filename)?.readText(Charsets.UTF_8).orEmpty()
