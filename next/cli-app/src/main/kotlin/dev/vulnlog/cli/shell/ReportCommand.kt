// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.core.collectReportingEntries
import dev.vulnlog.cli.core.mergeReportingEntries
import dev.vulnlog.cli.core.validateSharedProject
import dev.vulnlog.cli.parse.reporting.HtmlReportMapper.toDto
import dev.vulnlog.cli.parse.reporting.HtmlReportWriter.renderHtmlReport
import dev.vulnlog.cli.result.InputValidationResult
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.shell.shared.FilterOptions
import dev.vulnlog.cli.shell.shared.merge
import dev.vulnlog.cli.shell.shared.parseFiles
import dev.vulnlog.cli.shell.shared.parseStdin
import dev.vulnlog.cli.shell.shared.resolveFilter
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.cli.shell.shared.validateInputPath
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import kotlin.io.path.writeText

class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate a vulnerability report."

    val args: List<String> by argument().multiple()

    val output: String by option(
        "-o",
        "--output",
        help = "Output file path. Defaults to vulnlog-report.html in the current directory.",
    ).default("vulnlog-report.html")

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
        val reportContent = renderHtmlReport(reportData)

        val outputPath = Path.of(output)
        outputPath.writeText(reportContent)
        echo("Report written to: ${outputPath.toAbsolutePath()}")
    }

    private fun parseAndValidate(): Map<File, ParseResult.Ok> {
        val hasStdin = args.contains("-")
        val filePaths = args.filter { it != "-" }.map { Path.of(it) }

        filePaths.forEach { file ->
            val result = validateInputPath(file)
            if (result is InputValidationResult.Error) {
                echo(result.message, err = true)
                throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
            }
        }

        if (!hasStdin && filePaths.isEmpty()) {
            echo("Error: No input provided. Pass file paths or '-' for stdin.", err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val stdinResults = if (hasStdin) parseStdin() else null
        val fileResults = if (filePaths.isNotEmpty()) parseFiles(filePaths) else null

        val parseResults = merge(stdinResults, fileResults)

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
