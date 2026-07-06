// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.VulnlogFileContext
import dev.vulnlog.lib.core.formatSummary
import dev.vulnlog.lib.core.renderValidation
import dev.vulnlog.lib.core.validate
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationResult
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

/**
 * Renders one diagnostic line per validated file, stating the finding counts per severity. Shows
 * counts of findings the default output suppresses, such as warnings outside strict mode.
 */
fun renderValidationSummary(results: ValidationResults): List<String> =
    results.fileFindings
        .map { (input, result) -> renderValidationSummaryLine(input.sourceFile().name, result) }
        .sorted()

private fun renderValidationSummaryLine(
    name: String,
    result: ValidationResult,
): String {
    val counts =
        formatSummary(
            errors = result.errors.size,
            warnings = result.warnings.size,
            infos = result.infos.size,
        )
    return "validated $name: ${counts.ifEmpty { "no findings" }}"
}

/** Renders the findings of the given severities, one line per finding plus a summary, same format on all surfaces. */
fun renderValidationFindings(
    results: ValidationResults,
    renderedSeverities: Set<Severity> = Severity.entries.toSet(),
): String {
    val filtered =
        results.fileFindings
            .mapValues { (_, result) ->
                result.copy(findings = result.findings.filter { it.severity in renderedSeverities })
            }.filterValues { it.findings.isNotEmpty() }
    if (filtered.isEmpty()) return ""

    val lines = filtered.entries.flatMap { (input, result) -> renderValidation(input.sourceFile().name, result) }
    val findings = filtered.values.flatMap { it.findings }
    val summary =
        formatSummary(
            errors = findings.count { it.severity == Severity.ERROR },
            warnings = findings.count { it.severity == Severity.WARNING },
            infos = findings.count { it.severity == Severity.INFO },
        )
    return (lines + summary).joinToString("\n")
}
