// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.filter.resolveFilterOrFail
import dev.vulnlog.cli.shell.reporting.sharedProjectOrFail
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.collectReportingEntries
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.mergeReportingEntries
import dev.vulnlog.lib.core.renderReportingCounts
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.lib.parse.reporting.dto.FilterDataDto
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.nio.file.Path
import java.time.Instant

class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate a vulnerability report."

    val inputs: List<FileInputOption> by vulnlogFileInputs(
        "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    )

    val output: FileOutputOption by option(
        "-o",
        "--output",
        help = "Output file path, or '-' to write to stdout. Defaults to vulnlog-report.html in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vulnlog-report.html")))

    val filterOptions by FilterOptions()

    override fun run() {
        val validated: List<ValidVulnlogProject> = inputs.map { input -> validateInputOrFail(input).project }
        val files: List<VulnlogFile> = validated.map(ValidVulnlogProject::vulnlogProjectFile)
        val project = sharedProjectOrFail(files)

        val request = FilterRequest(filterOptions.reporter, filterOptions.releaseOption, filterOptions.tagsOptions)
        val filter = resolveFilterOrFail(request, files)

        val reported: List<ReportingEntry> = files.flatMap { collectReportingEntries(it.applyFilter(filter)) }
        val merged = mergeReportingEntries(reported)
        diagnosticSink().debug(renderReportingCounts(reported.size, merged.size))

        val filterData =
            FilterDataDto(
                release = filterOptions.releaseOption?.value,
                tags = filter.tags.map(Tag::value).sorted(),
                reporter = filter.reporter?.canonical(),
            )
        val inputNames = validated.map { it.inputDocument.filename }

        val reportData =
            toDto(
                project = project,
                entries = merged,
                generatedAt = Instant.now(),
                vulnlogVersion = BuildInfo.VERSION,
                inputs = inputNames,
                filter = filterData,
            )
        val content = renderHtmlReport(reportData)

        when (val target = output) {
            is FileOutputOption.File -> {
                writeReport(
                    { echoStatus(it) },
                    { echoMessage(it) },
                    target,
                    content,
                )
                diagnosticSink().verbose("wrote ${target.path}")
            }

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
