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
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag

class FilterOptions : OptionGroup() {
    val reporter: ReporterType? by option(
        "--reporter",
        help =
            """
            Filter on reporter.
            Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}
            """.trimIndent(),
    ).convert { parseReporter(it) }

    val releaseOption: Release? by option(
        "--release",
        help = "Filter on release, include all releases up to and including that release.",
    ).convert { Release(it) }

    val tagsOptions: Set<Tag> by option(
        "--tag",
        help = "Filter on tags. Use multiple times to filter on multiple tags.",
    ).convert { Tag(it) }
        .multiple()
        .unique()
}
