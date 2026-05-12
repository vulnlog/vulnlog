// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.reporting

import dev.vulnlog.lib.parse.reporting.dto.ReportDataDto
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

private const val TEMPLATE_PATH = "report/vulnlog-report-simple.html"
private const val DATA_PLACEHOLDER = "/*VULNLOG_DATA_PLACEHOLDER*/"

object HtmlReportWriter {
    /**
     * Renders a self-contained HTML report by loading the template from the classpath,
     * serializing the report data to JSON, and injecting it into the template.
     */
    fun renderHtmlReport(reportDataDto: ReportDataDto): String {
        val template = loadTemplate()
        val json = serializeToJson(reportDataDto)
        return template.replace(DATA_PLACEHOLDER, json)
    }

    private fun loadTemplate(): String {
        val classLoader = Thread.currentThread().contextClassLoader
        val stream =
            classLoader.getResourceAsStream(TEMPLATE_PATH)
                ?: error("Report template not found on classpath: $TEMPLATE_PATH")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun serializeToJson(data: ReportDataDto): String {
        val mapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        return mapper.writeValueAsString(data)
    }
}
