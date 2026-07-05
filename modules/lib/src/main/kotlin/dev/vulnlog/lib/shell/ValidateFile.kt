// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.VulnlogFileContext
import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationResults

/** Runs the domain rules over every file and returns the findings as data, keyed by input. */
fun validateFiles(fileToResult: Map<FileInputOption, ParseResult.Ok>): ValidationResults {
    val contexts =
        fileToResult.mapValues { (input, parsed) ->
            VulnlogFileContext(
                parsed.validationVersion,
                input.sourceFile().name,
                parsed.content,
                parsed.rootNode,
            )
        }
    val results = validate(contexts.values.toList())
    return ValidationResults(contexts.mapValues { (_, context) -> results.getValue(context) })
}

/** Renders the findings of the given severities, one block per file, same format on all surfaces. */
fun renderValidationFindings(
    results: ValidationResults,
    renderedSeverities: Set<Severity> = Severity.entries.toSet(),
): String =
    results.fileFindings
        .mapValues { (_, result) ->
            result.copy(findings = result.findings.filter { it.severity in renderedSeverities })
        }.filterValues { it.findings.isNotEmpty() }
        .entries
        .joinToString("\n\n") { (input, result) ->
            "Validation findings for ${input.sourceFile().name}:\n${renderValidation(result)}"
        }
