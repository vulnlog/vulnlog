// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.ReporterType

/** The filter flags shared by the commands that read Vulnlog files. Values are checked by resolveFilter. */
class FilterOptions : OptionGroup() {
    val reporterRequest: String? by option(
        "--reporter",
        metavar = "<reporter>",
        help =
            """
            Filter on reporter.
            Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}
            """.trimIndent(),
    )

    val asOfRequest: String? by option(
        "--as-of",
        metavar = "<release-id>",
        help =
            """
            Report the state as of this release.
            Every earlier release is included, and a fix that ships later counts as not yet fixed.
            """.trimIndent(),
    )

    val tagsRequest: Set<String> by option(
        "--tag",
        metavar = "<tag>",
        help = "Filter on tags. Use multiple times to filter on multiple tags.",
    ).multiple()
        .unique()
}

// TODO remove with 1.0.0

/** The filter flags that were renamed. Only the commands that carried the old spelling declare them. */
class RenamedFilterOptions : OptionGroup() {
    /** Renamed to `--as-of`. Kept so the old flag fails with a pointer instead of "no such option". */
    val releaseRequest: String? by option("--release", hidden = true)
}
