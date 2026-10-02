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
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.io.DirectoryOutputOption
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.InputSelectionResult
import dev.vulnlog.lib.io.InputValidationResult
import dev.vulnlog.lib.io.validateInputPath
import dev.vulnlog.lib.io.validateInputSelection
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.render.formatFailureLines
import dev.vulnlog.lib.render.renderWriteFailure
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private const val HELP_DISCUSSIONS_URL = "https://github.com/vulnlog/vulnlog/discussions/categories/q-a"

// TODO remove with 1.0.0, together with RenamedFilterOptions
fun CliktCommand.failOnRenamedFilterFlags(renamedFilterOptions: RenamedFilterOptions) {
    if (renamedFilterOptions.releaseRequest != null) {
        echoMessage(formatMessage(FindingSeverity.ERROR, "Option --release was renamed to --as-of."))
        echoMessage(
            formatHint("use '--as-of ${renamedFilterOptions.releaseRequest}' to report the state at that release"),
        )
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
    }
}

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

fun CliktCommand.writeOrFail(write: OutputWrite) {
    if (write is OutputWrite.Failed) {
        formatFailureLines(listOf(renderWriteFailure(write))).forEach(::echoMessage)
        throw ProgramResult(exitCode(write).code)
    }
}
