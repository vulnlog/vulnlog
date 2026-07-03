// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.model.VulnlogFileContext
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationResult
import dev.vulnlog.lib.result.ValidationResults

fun validateFiles(
    fileToResult: Map<FileInputOption, ParseResult.Ok>,
    renderedSeverities: Set<Severity> = Severity.entries.toSet(),
): ValidationResults {
    val contextToResults = validateEachFile(fileToResult)
    val renderedFindings =
        contextToResults
            .map { (context, findings) ->
                context.fileName to
                    findings.copy(findings = findings.findings.filter { it.severity in renderedSeverities })
            }.filter { (_, findings) -> findings.findings.isNotEmpty() }
            .joinToString("\n\n") { (filename, findings) ->
                "Validation findings for $filename:\n${renderValidation(findings)}"
            }
    return ValidationResults(
        renderedFindings = renderedFindings,
        hasErrors = contextToResults.values.any { it.errors.isNotEmpty() },
        hasWarnings = contextToResults.values.any { it.warnings.isNotEmpty() },
    )
}

private fun validateEachFile(
    fileToResult: Map<FileInputOption, ParseResult.Ok>,
): Map<VulnlogFileContext, ValidationResult> =
    fileToResult
        .map { (input, parseResult) ->
            VulnlogFileContext(
                parseResult.validationVersion,
                input.sourceFile().name,
                parseResult.content,
            )
        }.associateWith { context -> validate(context) }
