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
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import tools.jackson.databind.ObjectMapper
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
 * Builds a [VulnerabilityEntry] from [options] and serializes it as a YAML list item suitable for
 * pasting under a Vulnlog file's `vulnerabilities:` section. The verdict defaults to
 * [Verdict.UnderInvestigation]; if a reporter is supplied, a single report entry is added with the
 * current date.
 */
fun createVulnerabilityEntry(options: AddVulnerabilityOptions): String =
    serializeEntryYaml(V1Mapper.vulnerabilityToDto(buildVulnerabilityEntry(options)), createYamlMapper())

/**
 * Inserts or updates a vulnerability entry in [destinationContent].
 *
 * If no entry with [options.vulnId] exists in [destination], a new entry is inserted. In that
 * case an empty [options.releases] defaults to the latest published release of [destination]
 * (see [lastReleaseFavoringPublished]).
 *
 * If an entry with [options.vulnId] already exists, it is overwritten in place: only the fields
 * corresponding to options the user supplied are replaced; all other fields (including verdict,
 * name, description, analysis, resolution, comment, and aliases) are preserved. Position in the
 * file is preserved. On update an empty [options.releases] means "keep existing releases" — no
 * fallback to the latest release.
 *
 * Throws [IllegalArgumentException] if any release or tag in [options] is not defined in
 * [destination], or — on insert only — if [options.releases] is empty and [destination] has no
 * releases at all.
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

    val existing = destination.vulnerabilities.firstOrNull { it.id == options.vulnId }
    return if (existing != null) {
        updateExistingVulnerabilityEntry(existing, options, mapper, destinationContent)
    } else {
        // When the destination file does not have defined any releases
        // the new vulnerability entry has an empty releases field to be more tolerant.
        val effectiveReleases =
            options.releases.ifEmpty {
                val hasNoKnownReleases = knownReleases.isEmpty()
                if (hasNoKnownReleases) emptySet() else setOf(lastReleaseFavoringPublished(destination.releases))
            }
        addNewVulnerabilityEntryAtTop(effectiveReleases, options, mapper, destinationContent)
    }
}

private fun updateExistingVulnerabilityEntry(
    existing: VulnerabilityEntry,
    options: AddVulnerabilityOptions,
    mapper: ObjectMapper,
    destinationContent: String,
): AddOutcome {
    val merged = applyOptionsToExisting(existing, options)
    val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(merged), mapper)
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
): AddOutcome {
    val entry = buildVulnerabilityEntry(options.copy(releases = effectiveReleases))
    val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(entry), mapper)
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
