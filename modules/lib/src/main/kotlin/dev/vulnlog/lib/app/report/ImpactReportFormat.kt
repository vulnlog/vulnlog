// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

enum class ImpactReportFormat(
    val token: String,
    val fileExtension: String,
) {
    HTML("html", "html"),
    JSON("json", "json"),
    ;

    companion object {
        fun fromToken(token: String): ImpactReportFormat =
            entries.firstOrNull { it.token == token.lowercase() }
                ?: throw IllegalArgumentException(
                    "Unknown impact report format '$token'. Valid values: ${entries.joinToString(", ") { it.token }}.",
                )
    }
}
