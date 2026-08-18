// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.collectReportingEntries
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.mergeReportingEntries
import dev.vulnlog.lib.core.renderReportingCounts
import dev.vulnlog.lib.core.validateSharedProject
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
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

    val inputs: List<FileInputOption> by
        vulnlogFileInputs("Vulnlog file(s), or '-' to read from stdin, to create the report from.")

    val output: FileOutputOption by option(
        "-o",
        "--output",
        help = "Output file path, or '-' to write to stdout. Defaults to vulnlog-report.html in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vulnlog-report.html")))

    val filterOptions by FilterOptions()

    override fun run() {
        val validated: List<ValidVulnlogProject> =
            inputs.map { input -> validateInputOrFail(input).project }

        val vulnlogFiles: List<VulnlogFile> = validated.map { it.vulnlogProjectFile }
        val project =
            validateSharedProject(vulnlogFiles)
                ?: run {
                    echoMessage(
                        formatMessage(FindingSeverity.ERROR, "all input files must share the same project metadata"),
                    )
                    throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
                }

        val filter = resolveFilter(filterOptions, vulnlogFiles.first())

        val allEntries =
            vulnlogFiles.flatMap { collectReportingEntries(it, filter) }
        val merged = mergeReportingEntries(allEntries)
        diagnosticSink().debug(renderReportingCounts(allEntries.size, merged.size))

        val filterData =
            FilterDataDto(
                release = filterOptions.releaseOption,
                tags = filterOptions.tagsOptions.sorted(),
                reporter = filterOptions.reporter?.canonical(),
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
