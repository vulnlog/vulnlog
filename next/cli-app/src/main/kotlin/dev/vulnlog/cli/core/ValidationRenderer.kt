package dev.vulnlog.cli.core

import dev.vulnlog.cli.result.Severity
import dev.vulnlog.cli.result.ValidationFinding
import dev.vulnlog.cli.result.ValidationResult

/**
 * Renders a validation summary and details based on the provided validation result.
 * The method organizes findings by severity (errors, warnings, and infos)
 * and generates a string containing detailed messages followed by a summary.
 *
 * @param result The validation result containing findings to be rendered.
 * @return A string representation of the validation details and summary.
 */
fun renderValidation(result: ValidationResult): String =
    buildString {
        if (result.findings.isEmpty()) {
            append("OK — no issues found.")
            return@buildString
        }

        // Group by severity, errors first
        val grouped = result.findings.groupBy { it.severity }
        for (severity in listOf(Severity.ERROR, Severity.WARNING, Severity.INFO)) {
            val findings = grouped[severity] ?: continue
            findings.forEach { finding ->
                appendLine(renderFinding(finding))
            }
        }

        val summary =
            listOfNotNull(
                result.errors.size
                    .takeIf { it > 0 }
                    ?.let { "$it error(s)" },
                result.warnings.size
                    .takeIf { it > 0 }
                    ?.let { "$it warning(s)" },
                result.infos.size
                    .takeIf { it > 0 }
                    ?.let { "$it info(s)" },
            ).joinToString()
        append(summary)
    }

private fun renderFinding(finding: ValidationFinding): String {
    val prefix =
        when (finding.severity) {
            Severity.ERROR -> "ERROR"
            Severity.WARNING -> "WARN "
            Severity.INFO -> "INFO "
        }
    return "[$prefix] ${finding.path}: ${finding.message}"
}
