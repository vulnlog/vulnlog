// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.CanonicalYaml
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.nio.file.Path
import java.time.LocalDate

/**
 * The values an `add` invocation contributes to an entry. List-valued options are added to whatever an
 * existing entry already has; scalar options overwrite the existing value when supplied. `resolution` is
 * intentionally not settable here. The verdict, severity and justification are kept as raw strings and are
 * not cross-checked, so an inconsistent combination (e.g. `affected` with a justification) is accepted and
 * left for the validate command to flag.
 */
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
    val justification: String? = null,
    val comment: String? = null,
)

data class AddOutcome(
    val newContent: String,
    val vulnId: VulnId,
    val updated: Boolean,
)

/**
 * Builds an entry from [options] and serializes it as a `vulnerabilities:` list item for printing to STDOUT.
 * Absent options leave their fields empty; a reporter adds a report dated today.
 */
fun createVulnerabilityEntry(options: AddVulnerabilityOptions): String {
    val entry = mergeOptionsIntoEntry(emptyEntryDto(options.vulnId, options.releases), options)
    return CanonicalYaml.renderEntryListItem(entry, createYamlMapper())
}

/**
 * Inserts or updates the [options].vulnId entry in [destinationContent] and rewrites the whole document
 * in the canonical style ([YamlWriter.renderCanonicalDocument]), so any valid layout is accepted and a
 * subsequent `fmt` is a no-op. The optional `# $schema:` header is kept only when [destinationContent]
 * already had it; YAML comments in [destinationContent] do not survive.
 *
 * On insert, the new entry is placed at the top of the `vulnerabilities:` list; an empty
 * [options].releases defaults to the latest release of [destination], or stays empty when [destination]
 * defines no releases. On update, the entry keeps its position: list options are added to the existing
 * values, scalar options overwrite them, and omitted options are kept.
 *
 * Throws [IllegalArgumentException] if any release or tag in [options] is not defined in [destination].
 */
fun addVulnerabilityToFile(
    destination: VulnlogFile,
    destinationContent: VulnlogFileRaw,
    options: AddVulnerabilityOptions,
    mapper: ObjectMapper = createYamlMapper(),
): AddOutcome {
    val knownReleases = knownReleases(destination)
    val missingReleases = options.releases - knownReleases
    require(missingReleases.isEmpty()) {
        "Releases not defined in file: ${missingReleases.joinToString(", ") { it.value }}"
    }

    val missingTags = options.tags - knownTags(destination)
    require(missingTags.isEmpty()) {
        "Tags not defined in file: ${missingTags.joinToString(", ") { it.value }}"
    }

    val dto = mapper.readValue<VulnlogFileV1Dto>(destinationContent.content)
    val existing = dto.vulnerabilities.firstOrNull { parseVulnId(it.id) == options.vulnId }
    val (entries, updated) =
        if (existing != null) {
            val merged = mergeOptionsIntoEntry(existing, options)
            dto.vulnerabilities.map { if (it === existing) merged else it } to true
        } else {
            val effectiveReleases =
                options.releases.ifEmpty {
                    if (knownReleases.isEmpty()) emptySet() else setOf(destination.releases.last().id)
                }
            val entry = mergeOptionsIntoEntry(emptyEntryDto(options.vulnId, effectiveReleases), options)
            listOf(entry) + dto.vulnerabilities to false
        }
    val newContent =
        YamlWriter.renderCanonicalDocument(
            dto.copy(vulnerabilities = entries),
            mapper,
            includeSchemaHeader = hasSchemaHeader(destinationContent),
        )
    return AddOutcome(newContent, options.vulnId, updated)
}

/** Message stating whether [outcome] added a new entry to [destinationPath] or updated an existing one. */
fun formatAddOutcomeMessage(
    destinationPath: Path,
    outcome: AddOutcome,
): String =
    if (outcome.updated) {
        "Updated in $destinationPath: ${outcome.vulnId.id}"
    } else {
        "Added to $destinationPath: ${outcome.vulnId.id}"
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
): VulnerabilityEntryDto =
    base.copy(
        name = options.name ?: base.name,
        description = options.description ?: base.description,
        aliases = addDistinct(base.aliases, options.aliases.map { it.id }),
        releases = addDistinct(base.releases, options.releases.map { it.value }),
        packages = addDistinct(base.packages, options.packages.map { it.value }),
        reports = mergeReporters(base.reports, options.reporters),
        tags = addDistinct(base.tags, options.tags.map { it.value }),
        analysis = options.analysis ?: base.analysis,
        analyzedAt = options.analyzedAt ?: base.analyzedAt,
        verdict = options.verdict ?: base.verdict,
        severity = options.severity ?: base.severity,
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

/** Sets the date to today for each already-present reporter and appends a today-dated report for each new one. */
private fun mergeReporters(
    existing: List<ReportEntryDto>,
    reporters: Set<ReporterType>,
): List<ReportEntryDto> {
    if (reporters.isEmpty()) return existing
    val today = LocalDate.now()
    val byReporter = existing.associateByTo(LinkedHashMap()) { it.reporter }
    reporters.forEach { reporter ->
        val name = reporter.canonical()
        byReporter[name] = byReporter[name]?.copy(at = today) ?: ReportEntryDto(reporter = name, at = today)
    }
    return byReporter.values.toList()
}
