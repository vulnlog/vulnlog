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
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.reporting.collectReportingEntries
import dev.vulnlog.lib.core.reporting.mergeReportingEntries
import dev.vulnlog.lib.core.reporting.renderReportingCounts
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.model.report.WorkState
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.lib.parse.reporting.dto.FilterDataDto
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.nio.file.Path
import java.time.Instant

class ImpactReportCommand : CliktCommand(name = "impact") {
    override fun help(context: Context): String =
        "Generate an HTML report of how vulnerabilities affect this project. " +
            "Several Vulnlog files are merged into one report and must share the same project metadata."

    override fun helpEpilog(context: Context): String =
        """
        |Examples:
        |
        |Report every entry in the file.
        |
        |vulnlog report impact vulnlog.yaml
        |
        |Report the state as of release 1.1.0, which includes every earlier release.
        |
        |vulnlog report impact vulnlog.yaml --as-of 1.1.0
        |
        |Merge several files of the same project into one report.
        |
        |vulnlog report impact frontend.vl.yaml backend.vl.yaml
        """.trimMargin()

    val inputs: List<FileInputOption> by vulnlogFileInputs(
        "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    )

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help =
            "Output file path, or '-' to write to stdout. " +
                "Defaults to vulnlog-impact-report.html in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vulnlog-impact-report.html")))

    val filterOptions by FilterOptions()

    val impactFilterOptions by ImpactFilterOptions()

    override fun run() {
        val validated: List<ValidVulnlogProject> = inputs.map { input -> validateInputOrFail(input).project }
        val files: List<VulnlogFile> = validated.map(ValidVulnlogProject::vulnlogProjectFile)
        val project = sharedProjectOrFail(files)

        failOnRenamedFilterFlags(filterOptions)
        val request =
            FilterRequest(
                reporter = filterOptions.reporterRequest,
                asOf = filterOptions.asOfRequest,
                tags = filterOptions.tagsRequest,
                states = impactFilterOptions.statesRequest,
                verdicts = impactFilterOptions.verdictsRequest,
                dispositions = impactFilterOptions.dispositionsRequest,
            )
        val filter = resolveFilterOrFail(request, files)

        val reported: List<ReportingEntry> = files.flatMap { collectReportingEntries(it.applyFilter(filter)) }
        val merged = mergeReportingEntries(reported)
        diagnosticSink().debug(renderReportingCounts(reported.size, merged.size))

        val filterData =
            FilterDataDto(
                asOf = filterOptions.asOfRequest,
                tags = filter.tags.map(Tag::value).sorted(),
                reporter = filter.reporter?.canonical(),
                states = WorkState.entries.filter { it in filter.states }.map { it.canonical() },
                verdicts = VerdictKind.entries.filter { it in filter.verdicts }.map { it.canonical() },
                dispositions = Disposition.entries.filter { it in filter.dispositions }.map { canonical(it) },
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
