// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.BlockScalarStyle
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.detectBlockScalarStyles
import dev.vulnlog.lib.parse.v1.V1Mapper
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path
import java.time.LocalDate

data class AddVulnerabilityOptions(
    val vulnId: VulnId,
    val releases: Set<Release>,
    val packages: Set<Purl>,
    val tags: Set<Tag>,
    val reporter: ReporterType?,
)

data class AddOutcome(
    val newContent: String,
    val vulnId: VulnId,
    val updated: Boolean,
)

/**
 * Builds a [VulnerabilityEntry] from [options] and serializes it as a `vulnerabilities:` list item for
 * printing to STDOUT. The verdict defaults to [Verdict.UnderInvestigation]; a supplied reporter adds a
 * single report dated today.
 */
fun createVulnerabilityEntry(options: AddVulnerabilityOptions): String =
    serializeEntryYaml(V1Mapper.vulnerabilityToDto(buildVulnerabilityEntry(options)), createYamlMapper())

/**
 * Inserts or updates the [options].vulnId entry in [destinationContent], preserving the file's layout and
 * block-scalar styles (`|`/`>`) so reformatting stays a no-op.
 *
 * On insert, an empty [options].releases defaults to the latest published release (see
 * [lastReleaseFavoringPublished]), or stays empty when [destination] defines no releases. On update, the
 * entry is overwritten in place: only fields backing a supplied option are replaced, all others are kept,
 * and an empty [options].releases keeps the existing releases.
 *
 * Throws [IllegalArgumentException] if any release or tag in [options] is not defined in [destination].
 */
fun addVulnerabilityToFile(
    destination: VulnlogFile,
    destinationContent: String,
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

    val preservedStyles = detectBlockScalarStyles(VulnlogFileRaw(destinationContent))
    val existing = destination.vulnerabilities.firstOrNull { it.id == options.vulnId }
    return if (existing != null) {
        updateExistingVulnerabilityEntry(existing, options, mapper, destinationContent, preservedStyles)
    } else {
        val effectiveReleases =
            options.releases.ifEmpty {
                if (knownReleases.isEmpty()) emptySet() else setOf(destination.releases.last().id)
            }
        addNewVulnerabilityEntryAtTop(effectiveReleases, options, mapper, destinationContent, preservedStyles)
    }
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

private fun updateExistingVulnerabilityEntry(
    existing: VulnerabilityEntry,
    options: AddVulnerabilityOptions,
    mapper: ObjectMapper,
    destinationContent: String,
    preservedStyles: Map<String, BlockScalarStyle>,
): AddOutcome {
    val merged = applyOptionsToExisting(existing, options)
    val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(merged), mapper, preservedStyles)
    val newContent = replaceEntryById(destinationContent, options.vulnId, entryYaml)
    return AddOutcome(newContent, options.vulnId, updated = true)
}

private fun applyOptionsToExisting(
    existing: VulnerabilityEntry,
    options: AddVulnerabilityOptions,
): VulnerabilityEntry =
    existing.copy(
        releases = if (options.releases.isNotEmpty()) options.releases.toList() else existing.releases,
        packages = if (options.packages.isNotEmpty()) options.packages.toList() else existing.packages,
        tags = if (options.tags.isNotEmpty()) options.tags.toList() else existing.tags,
        reports = options.reporter?.let { mergeReporterIntoReports(existing.reports, it) } ?: existing.reports,
    )

private fun addNewVulnerabilityEntryAtTop(
    effectiveReleases: Set<Release>,
    options: AddVulnerabilityOptions,
    mapper: ObjectMapper,
    destinationContent: String,
    preservedStyles: Map<String, BlockScalarStyle>,
): AddOutcome {
    val entry = buildVulnerabilityEntry(options.copy(releases = effectiveReleases))
    val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(entry), mapper, preservedStyles)
    val newContent = insertEntryAfterVulnerabilitiesHeader(destinationContent, entryYaml)
    return AddOutcome(newContent, options.vulnId, updated = false)
}

private fun buildVulnerabilityEntry(options: AddVulnerabilityOptions): VulnerabilityEntry =
    VulnerabilityEntry(
        id = options.vulnId,
        releases = options.releases.toList(),
        packages = options.packages.toList(),
        reports =
            options.reporter?.let { listOf(ReportEntry(reporter = it, at = LocalDate.now())) }
                ?: emptyList(),
        tags = options.tags.toList(),
        verdict = Verdict.UnderInvestigation,
    )

private fun mergeReporterIntoReports(
    existing: List<ReportEntry>,
    reporter: ReporterType,
): List<ReportEntry> {
    val today = LocalDate.now()
    val index = existing.indexOfFirst { it.reporter == reporter }
    if (index == -1) {
        return existing + ReportEntry(reporter = reporter, at = today)
    }
    return existing.mapIndexed { i, entry -> if (i == index) entry.copy(at = today) else entry }
}
