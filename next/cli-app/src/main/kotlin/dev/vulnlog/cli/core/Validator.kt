package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.model.VulnlogFileContext
import dev.vulnlog.cli.result.Rule
import dev.vulnlog.cli.result.Severity
import dev.vulnlog.cli.result.ValidationFinding
import dev.vulnlog.cli.result.ValidationResult

/**
 * Validates the given vulnerability log file context and returns a validation result
 * containing findings such as errors, warnings, or informational messages.
 *
 * @param vulnlogContext The context of the vulnerability log file, including its
 *                       validation version, file name, and the actual vulnerability log file.
 * @return A `ValidationResult` containing a list of validation findings identified
 *         based on the specified validation version and the provided log file.
 */
fun validate(vulnlogContext: VulnlogFileContext): ValidationResult {
    val findings =
        when (vulnlogContext.validationVersion) {
            ParseValidationVersion.V1 -> v1Rules
        }.flatMap { rule -> rule(vulnlogContext.vulnlogFile) }
    return ValidationResult(findings)
}

private val v1Rules =
    listOf(
        ::validateUniqueReleaseIds,
        ::validateUniqueTagsIds,
        ::validateUniqueVulnerabilityIds,
        ::validateReleaseRefs,
        ::analyzeDateNotBeforeEarliestReportDate,
    )

private fun validateUniqueReleaseIds(file: VulnlogFile): List<ValidationFinding> {
    return file.releases
        .groupBy { it.id }
        .filter { (_, group) -> group.size > 1 }
        .map { (id, _) ->
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.DUPLICATE_RELEASE_ID,
                path = "releases[${id.value}]",
                message = "Duplicate release ID '$id'.",
            )
        }
}

private fun validateUniqueTagsIds(file: VulnlogFile): List<ValidationFinding> {
    return file.tags
        ?.groupBy { it.id }
        ?.filter { (_, group) -> group.size > 1 }
        ?.map { (id, _) ->
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.DUPLICATE_TAG_ID,
                path = "tags[${id.value}]",
                message = "Duplicate tag ID '$id'.",
            )
        } ?: emptyList()
}

private fun validateUniqueVulnerabilityIds(file: VulnlogFile): List<ValidationFinding> {
    return file.vulnerabilities
        .groupBy { it.id }
        .filter { (_, group) -> group.size > 1 }
        .map { (id, _) ->
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.DUPLICATE_VULNERABILITY_ID,
                path = "vulnerabilities[${id.canonical()}]",
                message = "Duplicate vulnerability ID '${id.canonical()}'.",
            )
        }
}

private fun validateReleaseRefs(file: VulnlogFile): List<ValidationFinding> {
    val definedIds = file.releases.map { it.id }.toSet()
    return file.vulnerabilities.flatMap { vuln ->
        vuln.releases
            .filter { it !in definedIds }
            .map { ref ->
                ValidationFinding(
                    severity = Severity.ERROR,
                    rule = Rule.DANGLING_RELEASE_REFERENCE,
                    path = "vulnerabilities[${vuln.id.canonical()}].releases",
                    message = "References undefined release '${ref.value}'. Defined releases: ${
                        definedIds.sortedBy { it.value }.joinToString { it.value }
                    }",
                )
            }
    }
}

private fun analyzeDateNotBeforeEarliestReportDate(file: VulnlogFile): List<ValidationFinding> {
    return file.vulnerabilities
        .mapNotNull { vuln ->
            val analyzedAt = vuln.analyzedAt ?: return@mapNotNull null
            val earliest = vuln.reports.mapNotNull { it.at }.minOrNull() ?: return@mapNotNull null
            if (earliest > analyzedAt) {
                ValidationFinding(
                    severity = Severity.WARNING,
                    rule = Rule.ANALYSED_BEFORE_REPORTED,
                    path = "vulnerabilities[${vuln.id.canonical()}].analyzed_at",
                    message = "Analyzed date '$analyzedAt' is before earliest reported date '$earliest'.",
                )
            } else {
                null
            }
        }
}
