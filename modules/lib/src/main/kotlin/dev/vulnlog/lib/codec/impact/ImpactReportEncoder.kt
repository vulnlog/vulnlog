// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.impact.dto.ImpactReportDto
import dev.vulnlog.lib.core.reporting.ImpactReport
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

private const val TEMPLATE_PATH = "report/vulnlog-report-simple.html"
private const val DATA_PLACEHOLDER = "/*VULNLOG_DATA_PLACEHOLDER*/"

object ImpactReportEncoder {
    fun encode(report: ImpactReport): String {
        val template = loadTemplate()
        val json = serializeToJson(ImpactReportMapper.toDto(report))
        return template.replace(DATA_PLACEHOLDER, escapeForScript(json))
    }

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

    private fun serializeToJson(data: ImpactReportDto): String {
        val mapper: ObjectMapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        return mapper.writeValueAsString(data)
    }
}
