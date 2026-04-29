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
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.shell.shared.FileInputOption
import dev.vulnlog.cli.shell.shared.parseInputs
import dev.vulnlog.cli.shell.shared.toInputFileOption
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import java.io.File

class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate Vulnlog YAML files and report issues."

    val inputs: List<FileInputOption> by argument(help = "Vulnlog file(s) to validate.")
        .convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)

    val strict: Boolean by option("--strict", help = "Treats warnings as errors.").flag(default = false)

    override fun run() {
        val parsedSuccessfully = parseInputOrFail(inputs)

        val validationFindings = validateFiles(parsedSuccessfully)
        if (validationFindings.renderedFindings.isNotBlank()) {
            echo(validationFindings.renderedFindings, err = true)
        }
        if (validationFindings.hasErrors || (validationFindings.hasWarnings && strict)) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        } else {
            echo("Validation OK")
        }
    }

    private fun parseInputOrFail(inputs: List<FileInputOption>): Map<File, ParseResult.Ok> {
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
}
