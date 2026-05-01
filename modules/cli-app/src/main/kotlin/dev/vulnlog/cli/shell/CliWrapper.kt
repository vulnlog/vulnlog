// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import dev.vulnlog.lib.result.InputValidationResult
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import dev.vulnlog.lib.result.ValidationResults
import dev.vulnlog.lib.shell.DirectoryOutputOption
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.parseInputs
import dev.vulnlog.lib.shell.resolveReleaseFilter
import dev.vulnlog.lib.shell.resolveTagsFilter
import dev.vulnlog.lib.shell.validateFiles
import dev.vulnlog.lib.shell.validateInputPath
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

fun CliktCommand.parseInputOrFail(inputs: List<FileInputOption>): Map<File, ParseResult.Ok> {
    val parseResults: ParseResults =
        try {
            parseInputs(inputs)
        } catch (e: IllegalArgumentException) {
            echo(e.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        } catch (e: IllegalStateException) {
            echo(e.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }
    parseResults.onEachFailure { file, result ->
        echo("Parsing of ${file.name} failed:", err = true)
        echo(result.error, err = true)
    }
    if (parseResults.failure.isNotEmpty()) {
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
    return parseResults.success
}

fun CliktCommand.validateParsedInputOrFailWithFailureOutput(
    fileToResult: Map<File, ParseResult.Ok>,
): ValidationResults {
    val validationFindings = validateFiles(fileToResult)
    if (validationFindings.renderedFindings.isNotBlank()) {
        echo(validationFindings.renderedFindings, err = true)
    }
    if (validationFindings.hasErrors) {
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

fun OptionCallTransformContext.toOutputDirectoryOption(output: String): DirectoryOutputOption =
    if (output == "-") {
        DirectoryOutputOption.Stdout
    } else {
        val outputPath = Path.of(output)
        if (!outputPath.isDirectory()) {
            fail("Output path '$outputPath' is not a directory.")
        }
        DirectoryOutputOption.Directory(outputPath)
    }

fun ArgumentTransformContext.toInputFileOption(input: String): FileInputOption =
    if (input == "-") {
        FileInputOption.Stdin
    } else {
        toInputFile(input)
    }

fun ArgumentTransformContext.toInputFile(input: String): FileInputOption.File {
    val inputPath = Path.of(input)
    if (inputPath.isDirectory() || !inputPath.exists()) {
        fail("Input path '$inputPath' is a directory or file does not exist.")
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
        VulnlogFilter(releases, tags, filterOptions.reporter)
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
        out("Vulnlog file created at: ${initFile.path.toAbsolutePath()}")
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}

fun writeSuppress(
    out: (String) -> Unit,
    err: (String) -> Unit,
    outputDir: DirectoryOutputOption.Directory,
    content: SuppressionFile,
) {
    val outputPath = outputDir.path.resolve(content.fileName)
    try {
        outputPath.writeText(content.content)
        out("Suppression file created at: ${outputPath.toAbsolutePath()}")
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
        out("Report written to: ${reportFile.path.toAbsolutePath()}")
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}
