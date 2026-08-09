// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatSummary
import dev.vulnlog.lib.core.shortenSchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.ParseFailure
import dev.vulnlog.lib.model.finding.ValidationFinding

/** One line per finding of a reported severity, plus a summary. Blank when nothing is reported. */
fun renderFindings(
    filename: String,
    findings: List<ValidationFinding>,
    reportedSeverities: Set<FindingSeverity> = FindingSeverity.entries.toSet(),
): String {
    val reported = findings.filter { it.severity in reportedSeverities }
    if (reported.isEmpty()) return ""

    val lines = reported.map { finding -> formatFinding(finding.severity, filename, finding.path, finding.message) }
    return (lines + summaryOf(reported)).joinToString("\n")
}

/** One error line per problem, naming the position in the document when it is known. */
fun renderProblem(
    filename: String,
    problem: ParseFailure,
): String {
    val position =
        listOfNotNull(
            problem.location?.let { "${it.line}:${it.column}" },
            problem.path,
        ).joinToString(": ")
    return formatFinding(FindingSeverity.ERROR, filename, position, problem.message)
}

/** The counts per severity for one file, including findings the output held back. */
fun renderValidationSummary(
    filename: String,
    findings: List<ValidationFinding>,
): String = "validated $filename: ${summaryOf(findings).ifEmpty { "no findings" }}"

/** States the schema version and entry counts of a file that was read successfully. */
fun renderParsedProject(
    filename: String,
    vulnlogProjectFile: VulnlogFile,
): String =
    "parsed $filename: schema version ${shortenSchemaVersion(vulnlogProjectFile.schemaVersion)}, " +
        "releases: ${vulnlogProjectFile.releases.size}, tags: ${vulnlogProjectFile.tags.size}, " +
        "vulnerabilities: ${vulnlogProjectFile.vulnerabilities.size}"

private fun summaryOf(findings: List<ValidationFinding>): String =
    formatSummary(
        errors = findings.count { it.severity == FindingSeverity.ERROR },
        warnings = findings.count { it.severity == FindingSeverity.WARNING },
        infos = findings.count { it.severity == FindingSeverity.INFO },
    )
