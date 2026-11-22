// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.report.reportJsonMapper
import dev.vulnlog.lib.codec.report.writeReportJson
import dev.vulnlog.lib.core.reporting.ImpactReport

private const val TEMPLATE_PATH = "report/vulnlog-report-simple.html"
private const val DATA_PLACEHOLDER = "/*VULNLOG_DATA_PLACEHOLDER*/"

object ImpactReportEncoder {
    fun encodeToJson(report: ImpactReport): String = writeReportJson(ImpactReportMapper.toDto(report))

    fun encodeToTable(
        report: ImpactReport,
        layout: TextLayout,
    ): String = ImpactReportTableView.render(report, layout)

    fun encodeToHtml(report: ImpactReport): String {
        val json = reportJsonMapper.writeValueAsString(ImpactReportMapper.toDto(report))
        return loadTemplate().replace(DATA_PLACEHOLDER, escapeForScript(json))
    }

    /** Text such as an analysis must not end the script element the data is embedded in. */
    private fun escapeForScript(json: String): String =
        json
            .replace("<", "\\u003c")
            .replace(">", "\\u003e")
            .replace("&", "\\u0026")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")

    private fun loadTemplate(): String {
        val classLoader = Thread.currentThread().contextClassLoader
        val stream =
            classLoader.getResourceAsStream(TEMPLATE_PATH)
                ?: error("Report template not found on classpath: $TEMPLATE_PATH")
        return stream.bufferedReader().use { it.readText() }
    }
}
