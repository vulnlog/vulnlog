// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.lib.core.collectReportingEntries
import dev.vulnlog.lib.core.mergeReportingEntries
import dev.vulnlog.lib.core.validateSharedProject
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.nio.file.Path
import java.time.LocalDate

class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate a vulnerability report."

    val inputs: List<FileInputOption> by argument(
        help = "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        help = "Output file path, or '-' to write to stdout. Defaults to vulnlog-report.html in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vulnlog-report.html")))

    val filterOptions by FilterOptions()

    override fun run() {
        val parsedSuccessfully = parseInputOrFail(inputs)
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val vulnlogFiles = parsedSuccessfully.values.map { it.content }

        val project =
            validateSharedProject(vulnlogFiles)
                ?: run {
                    echo("Error: All input files must share the same project metadata.", err = true)
                    throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
                }

        val filter = resolveFilter(filterOptions, vulnlogFiles.first())

        val allEntries =
            vulnlogFiles.flatMap { collectReportingEntries(it, filter) }
        val merged = mergeReportingEntries(allEntries)

        val reportData = toDto(project, merged, LocalDate.now())
        val content = renderHtmlReport(reportData)

        when (val target = output) {
            is FileOutputOption.File ->
                writeReport(
                    { echo(it) },
                    { echo(it, err = true) },
                    target,
                    content,
                )

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
