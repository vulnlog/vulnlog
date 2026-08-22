// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.renderFilterResolution
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import dev.vulnlog.lib.shell.DirectoryOutputOption
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.InputSelectionResult
import dev.vulnlog.lib.shell.InputValidationResult
import dev.vulnlog.lib.shell.resolveReleaseFilter
import dev.vulnlog.lib.shell.resolveReporterFilter
import dev.vulnlog.lib.shell.resolveTagsFilter
import dev.vulnlog.lib.shell.validateInputPath
import dev.vulnlog.lib.shell.validateInputSelection
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

private const val HELP_DISCUSSIONS_URL = "https://github.com/vulnlog/vulnlog/discussions/categories/q-a"

// TODO remove with 1.0.0

/** Fails when a renamed filter flag is used, naming the flag that replaced it. */
fun CliktCommand.failOnRenamedFilterFlags(renamedFilterOptions: RenamedFilterOptions) {
    if (renamedFilterOptions.releaseRequest != null) {
        echoMessage(formatMessage(FindingSeverity.ERROR, "Option --release was renamed to --as-of."))
        echoMessage(
            formatHint("use '--as-of ${renamedFilterOptions.releaseRequest}' to report the state at that release"),
        )
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
    }
}

/** Fails with a usage error when a command group is invoked without one of its subcommands. */
fun CliktCommand.requireSubcommand() {
    if (currentContext.invokedSubcommand == null) {
        throw PrintHelpMessage(currentContext, error = true, statusCode = ExitCode.GENERAL_ERROR.code)
    }
}

fun CliktCommand.echoHelpHint() {
    echoMessage(formatHint("ask for help at $HELP_DISCUSSIONS_URL"))
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

fun CliktCommand.vulnlogFileInputs(help: String) =
    argument(help = help)
        .convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)
        .validate { inputs ->
            val selection = validateInputSelection(inputs)
            if (selection is InputSelectionResult.Error) fail(selection.message)
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
): ResolvedFilter =
    try {
        val releases = resolveReleaseFilter(filterOptions.asOfRequest?.let(::Release), vulnlogFile)
        val tags = resolveTagsFilter(filterOptions.tagsRequest.map(::Tag).toSet(), vulnlogFile)
        val filter = ResolvedFilter(resolveReporterFilter(filterOptions.reporterRequest), releases, tags)
        renderFilterResolution(filter).forEach { diagnosticSink().verbose(it) }
        filter
    } catch (e: FilterValidationException) {
        echoMessage(formatMessage(FindingSeverity.ERROR, e.message.orEmpty()))
        echoMessage(formatHint(e.detail))
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
    } catch (e: IllegalArgumentException) {
        echoMessage(formatMessage(FindingSeverity.ERROR, "Invalid filter value: ${e.message}"))
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
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
        err(formatMessage(FindingSeverity.ERROR, "cannot write ${initFile.path}: ${e.message}"))
        throw ProgramResult(ExitCode.GENERAL_ERROR.code)
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
        err(formatMessage(FindingSeverity.ERROR, "cannot write $outputPath: ${e.message}"))
        throw ProgramResult(ExitCode.GENERAL_ERROR.code)
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
        err(formatMessage(FindingSeverity.ERROR, "cannot write ${reportFile.path}: ${e.message}"))
        throw ProgramResult(ExitCode.GENERAL_ERROR.code)
    }
}
