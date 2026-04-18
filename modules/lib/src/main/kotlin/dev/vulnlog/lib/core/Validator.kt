// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileContext
import dev.vulnlog.lib.result.Rule
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationFinding
import dev.vulnlog.lib.result.ValidationResult

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
        ::validateEveryReleaseIsReferenced,
        ::validateEveryTagIsReferenced,
        ::validateUniqueReleases,
        ::validateUniqueTags,
        ::validateUniqueVulnerabilities,
        ::validateVulnerabilityAliasNotReferencedInVulnerabilityIds,
        ::validateAliasNotReferencedInAliasIds,
        ::validateVulnerabilitiesReferenceValidReleases,
        ::validateAnalyzeDateNotBeforeEarliestReportDate,
        ::validateTagInReleasesIsDefined,
        ::validateTagInVulnerabilityIsDefined,
        ::validateSourceInReportIsDefinedWhenOther,
    )

private fun validateEveryReleaseIsReferenced(file: VulnlogFile): List<ValidationFinding> {
    val usedReleases = file.vulnerabilities.flatMap { vulnerability -> vulnerability.releases }.toSet()
    val usedReleasesInResolutions =
        file.vulnerabilities.map { vulnerability -> vulnerability.resolution?.release }.toSet()
    val allReferencedReleases = usedReleases.union(usedReleasesInResolutions)
    return file.releases
        .filter { release -> release.id !in allReferencedReleases }
        .map { release ->
            ValidationFinding(
                severity = Severity.INFO,
                rule = Rule.UNREFERENCED_RELEASE_ID,
                path = "releases[${release.id.value}]",
                message = "Unreferenced release ID '${release.id.value}'.",
            )
        }
}

private fun validateEveryTagIsReferenced(file: VulnlogFile): List<ValidationFinding> {
    val tagsFromReleases = file.releases.flatMap { release -> release.purls.flatMap { it.tags } }.toSet()
    val tagsFromVulnerabilities = file.vulnerabilities.flatMap { vulnerability -> vulnerability.tags }.toSet()
    val usedTags = tagsFromReleases.union(tagsFromVulnerabilities)
    return file.tags
        .filter { tag -> tag.id !in usedTags }
        .map { tag ->
            ValidationFinding(
                severity = Severity.INFO,
                rule = Rule.UNREFERENCED_TAG_ID,
                path = "tags[${tag.id.value}]",
                message = "Unreferenced tag ID '${tag.id.value}'.",
            )
        }
}

private fun validateUniqueReleases(file: VulnlogFile): List<ValidationFinding> =
    file.releases
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

private fun validateUniqueTags(file: VulnlogFile): List<ValidationFinding> =
    file.tags
        .groupBy { it.id }
        .filter { (_, group) -> group.size > 1 }
        .map { (id, _) ->
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.DUPLICATE_TAG_ID,
                path = "tags[${id.value}]",
                message = "Duplicate tag ID '$id'.",
            )
        }

private fun validateUniqueVulnerabilities(file: VulnlogFile): List<ValidationFinding> =
    file.vulnerabilities
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

private fun validateVulnerabilityAliasNotReferencedInVulnerabilityIds(file: VulnlogFile): List<ValidationFinding> {
    val allId = file.vulnerabilities.map { it.id }.toSet()
    return file.vulnerabilities
        .filter { vulnerability -> vulnerability.aliases.any { it in allId } }
        .map { vulnerability ->
            val duplicateAliases = vulnerability.aliases.filter { it in allId }.joinToString(", ") { it.canonical() }
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.DUPLICATE_VULNERABILITY_ID,
                path = "vulnerabilities[${vulnerability.id.canonical()}].aliases[$duplicateAliases]",
                message = "Duplicate aliases ID '$duplicateAliases'.",
            )
        }
}

private fun validateAliasNotReferencedInAliasIds(file: VulnlogFile): List<ValidationFinding> {
    val aliasToVulnerabilities = mutableMapOf<VulnId, MutableList<VulnId>>()
    file.vulnerabilities.forEach { vulnerability ->
        vulnerability.aliases.forEach { alias ->
            aliasToVulnerabilities.getOrPut(alias) { mutableListOf() }.add(vulnerability.id)
        }
    }
    return aliasToVulnerabilities
        .filter { (_, vulnIds) -> vulnIds.size > 1 }
        .flatMap { (aliasId, vulnIds) ->
            vulnIds.map { vulnId ->
                val ids = vulnIds.joinToString(", ") { it.canonical() }
                ValidationFinding(
                    severity = Severity.ERROR,
                    rule = Rule.DUPLICATE_VULNERABILITY_ID,
                    path = "vulnerabilities[${vulnId.canonical()}].aliases[${aliasId.canonical()}]",
                    message = "Alias ID '${aliasId.canonical()}' is referenced in multiple vulnerabilities: $ids.",
                )
            }
        }
}

private fun validateVulnerabilitiesReferenceValidReleases(file: VulnlogFile): List<ValidationFinding> {
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

private fun validateAnalyzeDateNotBeforeEarliestReportDate(file: VulnlogFile): List<ValidationFinding> {
    return file.vulnerabilities
        .mapNotNull { vuln ->
            val analyzedAt = vuln.analyzedAt ?: return@mapNotNull null
            val earliest = vuln.reports.mapNotNull { it.at }.minOrNull() ?: return@mapNotNull null
            if (earliest > analyzedAt) {
                ValidationFinding(
                    severity = Severity.WARNING,
                    rule = Rule.ANALYZED_BEFORE_REPORTED,
                    path = "vulnerabilities[${vuln.id.canonical()}].analyzed_at",
                    message = "Analyzed date '$analyzedAt' is before earliest reported date '$earliest'.",
                )
            } else {
                null
            }
        }
}

private fun validateTagInReleasesIsDefined(file: VulnlogFile): List<ValidationFinding> {
    val definedTags = file.tags.map { it.id }.toSet()
    return file.releases
        .flatMap { release ->
            release.purls.mapNotNull { purl ->
                val unknownTags = purl.tags.filter { it !in definedTags }
                if (unknownTags.isNotEmpty()) {
                    val unknownTagsString = unknownTags.joinToString(", ") { it.value }
                    ValidationFinding(
                        severity = Severity.ERROR,
                        rule = Rule.DANGLING_TAG_REFERENCE,
                        path = "releases[${release.id.value}].purls[${purl.purl.value}].tags[$unknownTagsString]",
                        message = "References undefined tags '$unknownTagsString'. Defined tags: ${
                            file.tags.map { it.id }.toSet().sortedBy { it.value }.joinToString { it.value }
                        }",
                    )
                } else {
                    null
                }
            }
        }
}

private fun validateTagInVulnerabilityIsDefined(file: VulnlogFile): List<ValidationFinding> {
    val definedTags = file.tags.map { it.id }.toSet()
    return file.vulnerabilities
        .mapNotNull { vuln ->
            val unknownTags = vuln.tags.filter { it !in definedTags }
            if (unknownTags.isNotEmpty()) {
                val unknownTagsString = unknownTags.joinToString(", ") { it.value }
                ValidationFinding(
                    severity = Severity.ERROR,
                    rule = Rule.DANGLING_TAG_REFERENCE,
                    path = "vulnerabilities[${vuln.id.canonical()}].tags[$unknownTagsString]",
                    message = "References undefined tags '$unknownTagsString'. Defined tags: ${
                        file.tags.map { it.id }.toSet().sortedBy { it.value }.joinToString { it.value }
                    }",
                )
            } else {
                null
            }
        }
}

private fun validateSourceInReportIsDefinedWhenOther(file: VulnlogFile): List<ValidationFinding> =
    file.vulnerabilities
        .filter { vuln -> vuln.reports.any { it.reporter == ReporterType.OTHER && it.source.isNullOrBlank() } }
        .map { vuln ->
            ValidationFinding(
                severity = Severity.ERROR,
                rule = Rule.MISSING_REPORTER_INFORMATION,
                path = "vulnerabilities[${vuln.id.canonical()}]",
                message = "Generic reporter without source specified.",
            )
        }
