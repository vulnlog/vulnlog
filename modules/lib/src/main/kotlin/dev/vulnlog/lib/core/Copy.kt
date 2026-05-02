// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import tools.jackson.databind.ObjectMapper

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
fun latestPublishedRelease(releases: List<ReleaseEntry>): Release =
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
