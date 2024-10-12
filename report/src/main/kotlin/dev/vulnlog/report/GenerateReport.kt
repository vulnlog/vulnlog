package dev.vulnlog.report

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun generateReport(
    jsonData: String,
    releaseBranchName: String,
    creationData: LocalDateTime,
): HtmlReport {
    val template = object {}.javaClass.getResource("/branch-template.html")?.readText(Charsets.UTF_8).orEmpty()

    val formatedDate = creationData.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val reportJsonData = "{ \"releaseBranchName\": \"$releaseBranchName\", \"generationDate\":\"$formatedDate\"}"
    val wrappedJsonReportData = "<script type=\"application/json\" id=\"report-data\">$reportJsonData</script>"
    val reportMarker = "    <!-- report-data -->"
    val reportData = template.replace(reportMarker, wrappedJsonReportData)

    val wrappedJsonVulnData = "<script type=\"application/json\" id=\"vuln-data\">$jsonData</script>"
    val vulnMarker = "    <!-- vuln-data -->"
    val vulnData = reportData.replace(vulnMarker, wrappedJsonVulnData)

    return HtmlReport(releaseBranchName, vulnData)
}
