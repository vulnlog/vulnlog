package dev.vulnlog.cli.shell.shared

import dev.vulnlog.cli.core.renderValidation
import dev.vulnlog.cli.core.validate
import dev.vulnlog.cli.model.VulnlogFileContext
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.result.ValidationResult
import dev.vulnlog.cli.result.ValidationResults
import java.io.File

fun validateFiles(fileToResult: Map<File, ParseResult.Ok>): ValidationResults {
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
