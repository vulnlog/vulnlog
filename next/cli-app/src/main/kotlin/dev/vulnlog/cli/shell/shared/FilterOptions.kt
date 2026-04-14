// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.cli.core.VulnlogFilter
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.shell.ExitCode

class FilterOptions : OptionGroup() {
    val reporter: ReporterType? by option(
        "--reporter",
        help =
            """
            Filter on reporter.
            Supported reporters: ${ReporterType.entries.joinToString(", ") { it.name.lowercase() }}
            """.trimIndent(),
    ).convert { ReporterType.valueOf(it.uppercase()) }

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

fun CliktCommand.resolveFilter(
    filterOptions: FilterOptions,
    vulnlogFile: VulnlogFile,
): VulnlogFilter =
    try {
        val releases = resolveReleaseFilter(filterOptions.releaseOption, vulnlogFile)
        val tags = resolveTagsFilter(filterOptions.tagsOptions, vulnlogFile)
        VulnlogFilter(releases, tags, filterOptions.reporter)
    } catch (e: FilterValidationException) {
        echo(e.message, err = true)
        echo(e.detail, err = true)
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
    }
