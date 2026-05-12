// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.model.ReporterType

class FilterOptions : OptionGroup() {
    val reporter: ReporterType? by option(
        "--reporter",
        help =
            """
            Filter on reporter.
            Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}
            """.trimIndent(),
    ).convert { parseReporter(it) }

    val releaseOption: String? by option(
        "--release",
        help = "Filter on release, include all releases up to and including that release.",
    )

    val tagsOptions: Set<String> by option(
        "--tag",
        help = "Filter on tags. Use multiple times to filter on multiple tags.",
    ).multiple()
        .unique()
}
