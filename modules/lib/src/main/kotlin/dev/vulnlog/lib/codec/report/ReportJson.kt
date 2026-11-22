// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.report

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/** One mapper for every report, so the JSON reports and the data in the HTML report are written alike. */
internal val reportJsonMapper: JsonMapper = JsonMapper.builder().addModule(kotlinModule()).build()

/** The [report] as indented JSON, for people who read the file and for diffs. */
internal fun writeReportJson(report: Any): String =
    reportJsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report)
