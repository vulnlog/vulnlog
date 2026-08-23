// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

/** The requested format for the changelog report. */
sealed interface ChangelogFormatRequest {
    val fileExtension: String

    data object Text : ChangelogFormatRequest {
        override val fileExtension: String = "txt"
    }

    data object Markdown : ChangelogFormatRequest {
        override val fileExtension: String = "md"
    }

    companion object {
        val byToken: Map<String, ChangelogFormatRequest> = mapOf("text" to Text, "markdown" to Markdown)

        fun fromToken(token: String): ChangelogFormatRequest =
            byToken[token.lowercase()]
                ?: throw IllegalArgumentException(
                    "Unknown changelog format '$token'. Valid values: ${byToken.keys.joinToString(", ")}.",
                )
    }
}
