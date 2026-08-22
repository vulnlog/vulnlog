// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

/** The requested format for the changelog report. */
sealed interface ChangelogFormatRequest {
    data object Text : ChangelogFormatRequest

    data object Markdown : ChangelogFormatRequest

    companion object {
        val byToken: Map<String, ChangelogFormatRequest> = mapOf("text" to Text, "markdown" to Markdown)
    }
}
