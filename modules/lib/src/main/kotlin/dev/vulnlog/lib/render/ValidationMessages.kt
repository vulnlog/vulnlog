// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatSummary
import dev.vulnlog.lib.core.shortenSchemaVersion
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.finding.ParseFailure
import dev.vulnlog.lib.finding.ValidationFinding
import dev.vulnlog.lib.model.VulnlogFile

/** Blank when no finding has a reported severity. */
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

/** Counts every finding, including those the output held back. */
fun renderValidationSummary(
    filename: String,
    findings: List<ValidationFinding>,
): String = "validated $filename: ${summaryOf(findings).ifEmpty { "no findings" }}"

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
