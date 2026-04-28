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
import dev.vulnlog.cli.shell.shared.FileInputOption
import dev.vulnlog.cli.shell.shared.FileOutputOption
import dev.vulnlog.cli.shell.shared.FilterOptions
import dev.vulnlog.cli.shell.shared.parseFiles
import dev.vulnlog.cli.shell.shared.parseStdin
import dev.vulnlog.cli.shell.shared.resolveFilter
import dev.vulnlog.cli.shell.shared.toInputFileOption
import dev.vulnlog.cli.shell.shared.toOutputFileOption
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.cli.shell.shared.writeReport
import dev.vulnlog.lib.core.collectReportingEntries
import dev.vulnlog.lib.core.mergeReportingEntries
import dev.vulnlog.lib.core.validateSharedProject
import dev.vulnlog.lib.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.lib.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import java.io.File
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
        val parseResults = parseAndValidate()
        val vulnlogFiles = parseResults.values.map { it.content }

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

    private fun parseAndValidate(): Map<File, ParseResult.Ok> {
        val parseResults: ParseResults =
            if (inputs.size == 1 && inputs.first() is FileInputOption.Stdin) {
                parseStdin()
            } else {
                if (inputs.any { it is FileInputOption.Stdin }) {
                    echo("Error: Mixing input files with STDIN is not allowed.", err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }

                val inputPaths = (inputs as List<FileInputOption.File>).map(FileInputOption.File::path)
                parseFiles(inputPaths)
            }
        parseResults.onEachFailure { file, result ->
            echo("Parsing of ${file.name} failed:", err = true)
            echo(result.error, err = true)
        }
        if (parseResults.failure.isNotEmpty()) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        val validationFindings = validateFiles(parseResults.success)
        if (validationFindings.renderedFindings.isNotBlank() && validationFindings.hasErrors) {
            echo(validationFindings.renderedFindings, err = true)
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }
        return parseResults.success
    }
}
