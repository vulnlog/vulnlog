// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import dev.vulnlog.lib.result.InputValidationResult
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationResults
import dev.vulnlog.lib.shell.DirectoryOutputOption
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.parseInputs
import dev.vulnlog.lib.shell.renderFilterResolution
import dev.vulnlog.lib.shell.renderParseFailures
import dev.vulnlog.lib.shell.renderParsedInputs
import dev.vulnlog.lib.shell.renderValidationFindings
import dev.vulnlog.lib.shell.renderValidationSummary
import dev.vulnlog.lib.shell.resolveReleaseFilter
import dev.vulnlog.lib.shell.resolveTagsFilter
import dev.vulnlog.lib.shell.validateFiles
import dev.vulnlog.lib.shell.validateInputPath
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

private const val HELP_DISCUSSIONS_URL = "https://github.com/vulnlog/vulnlog/discussions/categories/q-a"

fun CliktCommand.echoHelpHint() {
    echo("", err = true)
    echo("Stuck? Ask for help at $HELP_DISCUSSIONS_URL", err = true)
}

fun CliktCommand.parseInputOrFail(inputs: List<FileInputOption>): Map<FileInputOption, ParseResult.Ok> {
    val parseResults: ParseResults =
        try {
            parseInputs(inputs)
        } catch (e: RuntimeException) {
            if (e !is IllegalArgumentException && e !is IllegalStateException) throw e
            echo(e.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }
    if (parseResults.failure.isNotEmpty()) {
        renderParseFailures(parseResults).forEach { echo(it, err = true) }
        echoHelpHint()
        throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
    }
    renderParsedInputs(parseResults.success).forEach { diagnosticSink().verbose(it) }
    return parseResults.success
}

fun CliktCommand.validateParsedInputOrFailWithFailureOutput(
    fileToResult: Map<FileInputOption, ParseResult.Ok>,
    renderedSeverities: Set<Severity> = setOf(Severity.ERROR),
): ValidationResults {
    val validationFindings = validateFiles(fileToResult)
    renderValidationSummary(validationFindings).forEach { diagnosticSink().verbose(it) }
    val rendered = renderValidationFindings(validationFindings, renderedSeverities)
    if (rendered.isNotBlank()) {
        echo(rendered, err = true)
    }
    if (validationFindings.hasErrors) {
        echoHelpHint()
        throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
    }
    return validationFindings
}

fun OptionCallTransformContext.toOutputFileOption(output: String): FileOutputOption =
    if (output == "-") {
        FileOutputOption.Stdout
    } else {
        val outputPath = Path.of(output)
        if (outputPath.isDirectory()) {
            fail("Output path '$outputPath' is a directory, expected a file.")
        }
        FileOutputOption.File(outputPath)
    }

fun OptionCallTransformContext.toOutputDirectoryOption(output: String): DirectoryOutputOption {
    val outputPath = Path.of(output)
    if (!outputPath.isDirectory()) {
        fail("Output path '$outputPath' is not a directory.")
    }
    return DirectoryOutputOption.Directory(outputPath)
}

fun ArgumentTransformContext.toInputFileOption(input: String): FileInputOption =
    if (input == "-") {
        FileInputOption.Stdin
    } else {
        toInputFile(input)
    }

fun ArgumentTransformContext.toInputFile(input: String): FileInputOption.File {
    val inputPath = Path.of(input)
    if (!inputPath.exists()) {
        fail("Input path '$inputPath' does not exist.")
    }
    if (inputPath.isDirectory()) {
        fail("Input path '$inputPath' is a directory, expected a file.")
    }
    val inputFileValidation = validateInputPath(inputPath)
    if (inputFileValidation is InputValidationResult.Error) {
        fail("Input '$inputPath' is not valid: ${inputFileValidation.message}")
    }
    return FileInputOption.File(inputPath)
}

fun CliktCommand.resolveFilter(
    filterOptions: FilterOptions,
    vulnlogFile: VulnlogFile,
): VulnlogFilter =
    try {
        val releases = resolveReleaseFilter(filterOptions.releaseOption, vulnlogFile)
        val tags = resolveTagsFilter(filterOptions.tagsOptions, vulnlogFile)
        val filter = VulnlogFilter(releases, tags, filterOptions.reporter)
        renderFilterResolution(filter).forEach { diagnosticSink().verbose(it) }
        filter
    } catch (e: FilterValidationException) {
        echo(e.message, err = true)
        echo(e.detail, err = true)
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.ordinal)
    }

fun writeInit(
    out: (String) -> Unit,
    err: (String) -> Unit,
    initFile: FileOutputOption.File,
    content: String,
) {
    try {
        initFile.path.writeText(content)
        out(formatStatus(StatusVerb.CREATED, initFile.path.toString()))
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}

fun writeSuppressionFile(
    out: (String) -> Unit,
    err: (String) -> Unit,
    outputPath: Path,
    suppressionFile: SuppressionFile,
) {
    try {
        outputPath.writeText(suppressionFile.content)
        out(formatStatus(StatusVerb.WROTE, outputPath.toString()))
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}

fun writeReport(
    out: (String) -> Unit,
    err: (String) -> Unit,
    reportFile: FileOutputOption.File,
    content: String,
) {
    try {
        reportFile.path.writeText(content)
        out(formatStatus(StatusVerb.WROTE, reportFile.path.toString()))
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}
