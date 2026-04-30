// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.model.VulnlogFileContext
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ValidationResult
import dev.vulnlog.lib.result.ValidationResults
import java.io.File

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

private fun validateFiles(fileToResult: Map<File, ParseResult.Ok>): ValidationResults {
    val contextToResults = validateEachFile(fileToResult)
    val renderedFindings =
        contextToResults
            .filter { (_, findings) -> findings.findings.isNotEmpty() }
            .map { (context, findings) -> context.fileName to renderValidation(findings) }
            .joinToString("\n\n") { (filename, results) -> "Validation findings for $filename:\n$results" }
    return ValidationResults(
        renderedFindings = renderedFindings,
        hasErrors = contextToResults.values.any { it.errors.isNotEmpty() },
        hasWarnings = contextToResults.values.any { it.warnings.isNotEmpty() },
    )
}

private fun validateEachFile(fileToResult: Map<File, ParseResult.Ok>): Map<VulnlogFileContext, ValidationResult> =
    fileToResult
        .map { (file, parseResult) ->
            VulnlogFileContext(
                parseResult.validationVersion,
                file.name,
                parseResult.content,
            )
        }.associateWith { context -> validate(context) }
