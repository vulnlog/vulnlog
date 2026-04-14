// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.core.SuppressionFilter
import dev.vulnlog.cli.core.collectSuppressedVulnerabilities
import dev.vulnlog.cli.core.mapToSuppression
import dev.vulnlog.cli.parse.suppression.SuppressionWriter.writeSuppressionOutput
import dev.vulnlog.cli.result.InputValidationResult
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.shell.shared.FilterOptions
import dev.vulnlog.cli.shell.shared.parseFile
import dev.vulnlog.cli.shell.shared.parseStdin
import dev.vulnlog.cli.shell.shared.resolveFilter
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.cli.shell.shared.validateInputPath
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

class SuppressCommand : CliktCommand(name = "suppress") {
    override fun help(context: Context): String = "Create suppression files."

    val file: String by argument()
    val output: String? by option(
        "-o",
        "--output",
        help = "Output directory, or '-' to write to stdout. Defaults to current directory.",
    )

    val filterOptions by FilterOptions()

    override fun run() {
        val parseResults = parseAndValidate()
        val vulnlogFile = parseResults.values.first().content

        val filter = resolveFilter(filterOptions, vulnlogFile)

        val targetReporters =
            vulnlogFile.vulnerabilities
                .flatMap { it.reports }
                .map { it.reporter }
                .filter { filter.reporter == null || it == filter.reporter }
                .toSet()

        val suppressionVulns =
            collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(filter))
        val outputSuppressions = mapToSuppression(targetReporters, suppressionVulns)

        val writeToStdout = output == "-"
        if (writeToStdout && outputSuppressions.size > 1) {
            echo(
                "Error: Cannot write multiple suppression files to stdout. Use --reporter to select a single reporter.",
                err = true,
            )
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val outputDir =
            if (!writeToStdout) {
                val dir = Path.of(output ?: System.getProperty("user.dir"))
                if (!dir.toFile().isDirectory) {
                    echo("Error: Output path '$dir' is not a directory.", err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }
                dir
            } else {
                null
            }

        outputSuppressions.forEach { suppressionOutput ->
            val file = writeSuppressionOutput(suppressionOutput)
            if (writeToStdout) {
                echo(file.content)
            } else {
                val outputPath = outputDir!!.resolve(file.fileName)
                outputPath.writeText(file.content)
                echo("Suppression file created at: ${outputPath.toAbsolutePath()}")
            }
        }
    }

    private fun parseAndValidate(): Map<File, ParseResult.Ok> {
        val parseResults =
            if (file == "-") {
                parseStdin()
            } else {
                val result = validateInputPath(Path.of(file))
                if (result is InputValidationResult.Error) {
                    echo(result.message, err = true)
                    throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                }
                parseFile((result as InputValidationResult.Ok).path)
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
