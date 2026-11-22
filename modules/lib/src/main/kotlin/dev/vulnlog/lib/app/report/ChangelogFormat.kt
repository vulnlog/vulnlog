// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

enum class ChangelogFormat(
    val token: String,
    val fileExtension: String,
) {
    TEXT("text", "txt"),
    MARKDOWN("markdown", "md"),
    ;

    companion object {
        fun fromToken(token: String): ChangelogFormat =
            entries.firstOrNull { it.token == token.lowercase() }
                ?: throw IllegalArgumentException(
                    "Unknown changelog format '$token'. Valid values: ${entries.joinToString(", ") { it.token }}.",
                )
    }
}
