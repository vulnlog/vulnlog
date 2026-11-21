// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.core.reporting.ImpactReport
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

private const val TEMPLATE_PATH = "report/vulnlog-report-simple.html"
private const val DATA_PLACEHOLDER = "/*VULNLOG_DATA_PLACEHOLDER*/"

object ImpactReportEncoder {
    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()

    fun encodeToJson(report: ImpactReport): String =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ImpactReportMapper.toDto(report))

    fun encodeToHtml(report: ImpactReport): String {
        val json = mapper.writeValueAsString(ImpactReportMapper.toDto(report))
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
