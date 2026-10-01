// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.document.dto.ReportEntryDto
import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.CanonicalYaml
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.document.yaml.hasSchemaHeader
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import java.nio.file.Path
import java.time.LocalDate

data class AddVulnerabilityOptions(
    val vulnId: VulnId,
    val name: String? = null,
    val aliases: Set<VulnId> = emptySet(),
    val releases: Set<Release> = emptySet(),
    val packages: Set<Purl> = emptySet(),
    val tags: Set<Tag> = emptySet(),
    val reporters: Set<ReporterType> = emptySet(),
    val description: String? = null,
    val analysis: String? = null,
    val analyzedAt: LocalDate? = null,
    val verdict: String? = null,
    val severity: String? = null,
    val disposition: String? = null,
    val justification: String? = null,
    val comment: String? = null,
)

data class AddOutcome(
    val newContent: String,
    val vulnId: VulnId,
    val updated: Boolean,
)

fun createVulnerabilityEntry(
    options: AddVulnerabilityOptions,
    today: LocalDate,
): String {
    val entry = mergeOptionsIntoEntry(emptyEntryDto(options.vulnId, options.releases), options, today)
    return CanonicalYaml.renderEntryListItem(entry)
}

/**
 * Rewrites the whole document canonically, so a later `fmt` changes nothing; YAML comments do not survive, the
 * `# $schema:` header only when it was there. A new entry goes to the top and defaults to the latest release; an
 * updated one keeps its place.
 *
 * Throws [IllegalArgumentException] for a release or tag the destination does not define.
 */
fun addVulnerabilityToFile(
    destination: ValidVulnlogProject,
    options: AddVulnerabilityOptions,
    today: LocalDate,
): AddOutcome {
    val destinationFile = destination.vulnlogProjectFile
    val knownReleases = knownReleases(destinationFile)
    val missingReleases = options.releases - knownReleases
    require(missingReleases.isEmpty()) {
        "Releases not defined in file: ${missingReleases.joinToString(", ") { it.value }}"
    }

    val missingTags = options.tags - knownTags(destinationFile)
    require(missingTags.isEmpty()) {
        "Tags not defined in file: ${missingTags.joinToString(", ") { it.value }}"
    }

    val dto =
        when (destination.parsedVulnlogProject.validatedDto) {
            is VulnlogFileV1Dto -> destination.parsedVulnlogProject.validatedDto
        }
    val existing = dto.vulnerabilities.firstOrNull { parseVulnId(it.id) == options.vulnId }
    val (entries, updated) =
        if (existing != null) {
            val merged = mergeOptionsIntoEntry(existing, options, today)
            dto.vulnerabilities.map { if (it === existing) merged else it } to true
        } else {
            val effectiveReleases =
                options.releases.ifEmpty {
                    if (knownReleases.isEmpty()) {
                        emptySet()
                    } else {
                        setOf(
                            destinationFile.releases
                                .last()
                                .id,
                        )
                    }
                }
            val entry = mergeOptionsIntoEntry(emptyEntryDto(options.vulnId, effectiveReleases), options, today)
            listOf(entry) + dto.vulnerabilities to false
        }
    val newContent =
        YamlWriter.renderCanonicalDocument(
            dto.copy(vulnerabilities = entries),
            includeSchemaHeader = hasSchemaHeader(destination.nodeTree.rootNode),
        )
    return AddOutcome(newContent, options.vulnId, updated)
}

fun formatAddOutcomeMessage(
    destinationPath: Path,
    outcome: AddOutcome,
): String =
    if (outcome.updated) {
        formatStatus(StatusVerb.UPDATED, "${outcome.vulnId.id} in $destinationPath")
    } else {
        formatStatus(StatusVerb.ADDED, "${outcome.vulnId.id} to $destinationPath")
    }

private fun emptyEntryDto(
    vulnId: VulnId,
    releases: Collection<Release>,
): VulnerabilityEntryDto =
    VulnerabilityEntryDto(
        id = vulnId.id,
        releases = releases.map { it.value },
        packages = emptyList(),
        reports = emptyList(),
    )

/** Adds [options] onto [base]: lists are unioned, scalars overwrite when supplied, reports merge by reporter. */
private fun mergeOptionsIntoEntry(
    base: VulnerabilityEntryDto,
    options: AddVulnerabilityOptions,
    today: LocalDate,
): VulnerabilityEntryDto =
    base.copy(
        name = options.name ?: base.name,
        description = options.description ?: base.description,
        aliases = addDistinct(base.aliases, options.aliases.map { it.id }),
        releases = addDistinct(base.releases, options.releases.map { it.value }),
        packages = addDistinct(base.packages, options.packages.map { it.value }),
        reports = mergeReporters(base.reports, options.reporters, today),
        tags = addDistinct(base.tags, options.tags.map { it.value }),
        analysis = options.analysis ?: base.analysis,
        analyzedAt = options.analyzedAt ?: base.analyzedAt,
        verdict = options.verdict ?: base.verdict,
        severity = options.severity ?: base.severity,
        disposition = options.disposition ?: base.disposition,
        justification = options.justification ?: base.justification,
        comment = options.comment ?: base.comment,
    )

private fun addDistinct(
    existing: List<String>,
    added: List<String>,
): List<String> {
    val result = existing.toMutableList()
    added.forEach { if (it !in result) result.add(it) }
    return result
}

private fun mergeReporters(
    existing: List<ReportEntryDto>,
    reporters: Set<ReporterType>,
    today: LocalDate,
): List<ReportEntryDto> {
    if (reporters.isEmpty()) return existing
    val byReporter = existing.associateByTo(LinkedHashMap()) { it.reporter }
    reporters.forEach { reporter ->
        val name = reporter.canonical()
        byReporter[name] = byReporter[name]?.copy(at = today) ?: ReportEntryDto(reporter = name, at = today)
    }
    return byReporter.values.toList()
}
