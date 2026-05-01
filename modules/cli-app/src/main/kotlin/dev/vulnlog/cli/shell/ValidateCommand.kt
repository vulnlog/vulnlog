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
import dev.vulnlog.lib.shell.FileInputOption

class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate Vulnlog YAML files and report issues."

    val inputs: List<FileInputOption> by argument(help = "Vulnlog file(s) to validate.")
        .convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)

    val strict: Boolean by option("--strict", help = "Treats warnings as errors.").flag(default = false)

    override fun run() {
        val parsedSuccessfully = parseInputOrFail(inputs)
        val validationFindings = validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        if (validationFindings.hasWarnings && strict) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }
        echo("Validation OK")
    }
}
