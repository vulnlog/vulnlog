// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path

data class CopyOutcome(
    val copied: List<VulnId>,
    val newContent: String,
)

/**
 * Copies the requested vulnerability entries from [source] into [destinationContent].
 * If an entry already exists in [destination], it is merged with the source entry: existing values win for
 * scalars; lists (aliases, packages, tags) are unioned; reports are unioned by reporter. Releases on every
 * copied entry are rewritten to the destination's latest release (favoring published).
 */
fun copyVulnerabilities(
    source: VulnlogFile,
    destination: VulnlogFile,
    destinationContent: String,
    vulnIds: Set<VulnId>,
    mapper: ObjectMapper = createYamlMapper(),
): CopyOutcome {
    val release = lastReleaseFavoringPublished(destination.releases)
    val sourceEntries = source.vulnerabilities.filter { it.id in vulnIds }
    val existingById = destination.vulnerabilities.associateBy { it.id }

    val newContent =
        sourceEntries.fold(destinationContent) { acc, incoming ->
            val existing = existingById[incoming.id]
            val merged = mergeVulnerabilityEntry(existing, incoming, release)
            val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(merged), mapper)
            if (existing == null) {
                insertEntryAfterVulnerabilitiesHeader(acc, entryYaml)
            } else {
                replaceEntryById(acc, incoming.id, entryYaml)
            }
        }
    return CopyOutcome(
        copied = sourceEntries.map { it.id },
        newContent = newContent,
    )
}

private fun mergeVulnerabilityEntry(
    existing: VulnerabilityEntry?,
    incoming: VulnerabilityEntry,
    release: Release,
): VulnerabilityEntry {
    if (existing == null) return incoming.copy(releases = listOf(release))
    return existing.copy(
        name = existing.name ?: incoming.name,
        aliases = unionPreservingOrder(existing.aliases, incoming.aliases),
        releases = listOf(release),
        description = existing.description ?: incoming.description,
        packages = unionPreservingOrder(existing.packages, incoming.packages),
        reports = mergeReports(existing.reports, incoming.reports),
        tags = unionPreservingOrder(existing.tags, incoming.tags),
        analysis = existing.analysis ?: incoming.analysis,
        analyzedAt = existing.analyzedAt ?: incoming.analyzedAt,
        resolution = existing.resolution ?: incoming.resolution,
        comment = existing.comment ?: incoming.comment,
    )
}

private fun <T> unionPreservingOrder(
    first: List<T>,
    second: List<T>,
): List<T> {
    val seen = first.toMutableSet()
    return first + second.filter { seen.add(it) }
}

private fun mergeReports(
    existing: List<ReportEntry>,
    incoming: List<ReportEntry>,
): List<ReportEntry> {
    val byReporter = LinkedHashMap<ReporterType, ReportEntry>()
    existing.forEach { byReporter[it.reporter] = it }
    incoming.forEach { incomingReport ->
        byReporter.merge(incomingReport.reporter, incomingReport) { e, n ->
            e.copy(
                at = e.at ?: n.at,
                source = e.source ?: n.source,
                vulnIds = e.vulnIds + n.vulnIds,
                suppress = e.suppress ?: n.suppress,
            )
        }
    }
    return byReporter.values.toList()
}

fun formatVulnIdsNotInSourceMessage(missing: Set<VulnId>): String =
    "Error: Vulnerability IDs not found in source file: ${missing.joinToString(", ") { it.id }}"

fun formatCopiedMessage(
    destinationPath: Path,
    ids: List<VulnId>,
): String =
    if (ids.isEmpty()) {
        "No new vulnerabilities to copy to $destinationPath"
    } else {
        "Copied to $destinationPath: ${ids.joinToString(", ") { it.id }}"
    }

/**
 * Identifies vulnerability IDs from a given list that do not exist in the list of known vulnerabilities.
 *
 * @param vulnerabilities A list of known vulnerability entries, where each entry includes metadata and a unique ID.
 * @param vulnIds A list of vulnerability ID strings to be checked against the known vulnerabilities.
 * @return A set of parsed `VulnId` objects that are not present in the provided list of known vulnerabilities.
 */
fun findNonExistingVulnIds(
    vulnerabilities: List<VulnerabilityEntry>,
    vulnIds: Set<VulnId>,
): Set<VulnId> =
    vulnIds
        .filter { vulnId -> vulnId !in vulnerabilities.map(VulnerabilityEntry::id) }
        .toSet()

/**
 * Finds the latest published release from a list of release entries.
 *
 * @param releases The list of release entries to search. Releases with a null publication date are considered unpublished.
 * @return The most recently published release or the last entry if no release has been published.
 */
fun lastReleaseFavoringPublished(releases: List<ReleaseEntry>): Release =
    releases.lastOrNull { it.publicationDate != null }?.id ?: releases.last().id

/**
 * Serializes a [VulnerabilityEntryDto] object to a YAML-compatible string representation
 * with specific indentation adjustments for proper formatting.
 *
 * The YAML format adjusts the first line with a prefix `-` and indents subsequent lines with spaces.
 * This ensures that the resulting YAML is appropriately nested for inclusion in larger YAML structures.
 *
 * @param entry The [VulnerabilityEntryDto] object to be serialized.
 * @param mapper An instance of [ObjectMapper] used for converting the entry object into a raw JSON string.
 * @return A string representing the YAML-compatible serialized format of the given entry.
 */
fun serializeEntryYaml(
    entry: VulnerabilityEntryDto,
    mapper: ObjectMapper,
): String {
    val raw = mapper.writeValueAsString(entry)
    val lines =
        raw
            .lines()
            .dropWhile { it.trimStart().startsWith("---") || it.isBlank() }
            .dropLastWhile { it.isBlank() }

    return lines
        .mapIndexed { index, line ->
            if (index == 0) "  - $line" else "    $line"
        }.joinToString("\n")
}

/**
 * Inserts a YAML entry immediately after the "vulnerabilities:" header in the given file content.
 * If the "vulnerabilities:" header is empty (e.g., "vulnerabilities: []"), it converts it to "vulnerabilities:".
 *
 * @param fileContent The content of the file as a string, expected to include the "vulnerabilities:" section.
 * @param entryYaml The YAML entry to be inserted after the "vulnerabilities:" header.
 * @return The updated file content with the YAML entry inserted after the "vulnerabilities:" header.
 * @throws IllegalStateException If the "vulnerabilities:" section is not found in the file content.
 */
fun insertEntryAfterVulnerabilitiesHeader(
    fileContent: String,
    entryYaml: String,
): String {
    val lines = fileContent.lines().toMutableList()
    val headerIndex =
        lines.indexOfFirst { it.trimEnd() == "vulnerabilities:" || it.trimEnd() == "vulnerabilities: []" }
    if (headerIndex == -1) {
        error("No 'vulnerabilities:' section found in target file")
    }

    if (lines[headerIndex].trimEnd() == "vulnerabilities: []") {
        lines[headerIndex] = "vulnerabilities:"
    }

    lines.add(headerIndex + 1, "")
    lines.add(headerIndex + 2, entryYaml)

    return lines.joinToString("\n")
}

private val ENTRY_ID_LINE = Regex("""^  - id:\s+["']?(\S+?)["']?\s*$""")

/**
 * Replaces the YAML block of the entry whose `id` matches [vulnId] with [newEntryYaml].
 * If no such entry is found, falls back to inserting [newEntryYaml] after the `vulnerabilities:` header.
 *
 * The block is delimited by the next `  - id:` line, the next top-level YAML key, or the end of the file.
 * Trailing blank lines that visually separate this entry from the next are preserved as-is.
 */
fun replaceEntryById(
    fileContent: String,
    vulnId: VulnId,
    newEntryYaml: String,
): String {
    val lines = fileContent.lines()
    val startIndex =
        lines.indexOfFirst { line ->
            ENTRY_ID_LINE.matchEntire(line)?.groupValues?.get(1) == vulnId.id
        }
    if (startIndex == -1) {
        return insertEntryAfterVulnerabilitiesHeader(fileContent, newEntryYaml)
    }
    var endIndex = lines.size
    for (i in startIndex + 1 until lines.size) {
        val line = lines[i]
        if (ENTRY_ID_LINE.matches(line)) {
            endIndex = i
            break
        }
        // top-level YAML key (non-indented, non-blank, non-comment) ends the vulnerabilities section
        if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("#")) {
            endIndex = i
            break
        }
    }
    // Walk back over blank separator lines so they remain between this entry and the next
    while (endIndex > startIndex + 1 && lines[endIndex - 1].isBlank()) {
        endIndex--
    }

    val rebuilt = lines.subList(0, startIndex) + newEntryYaml.lines() + lines.subList(endIndex, lines.size)
    return rebuilt.joinToString("\n")
}
