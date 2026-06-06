// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.BlockScalarStyle
import dev.vulnlog.lib.parse.CanonicalYaml
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.detectBlockScalarStyles
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path

data class CopyOutcome(
    val copied: List<VulnId>,
    val newContent: VulnlogFileRaw,
)

/**
 * Copies the requested vulnerability entries from [source] into [destinationContent].
 * If an entry already exists in [destination], it is merged with the source entry: existing values win for
 * scalars; lists (aliases, packages, tags) are unioned; reports are unioned by reporter. Releases on every
 * copied entry are rewritten to the destination's latest release (favoring published).
 *
 * Block-scalar styles (`|`/`>`) are preserved from the source and destination text; on a value collision the
 * destination wins, matching the existing-scalars-win merge rule.
 */
fun copyVulnerabilities(
    source: VulnlogFile,
    destination: VulnlogFile,
    sourceContent: VulnlogFileRaw = VulnlogFileRaw(""),
    destinationContent: VulnlogFileRaw,
    vulnIds: Set<VulnId>,
    mapper: ObjectMapper = createYamlMapper(),
): CopyOutcome {
    val release = lastReleaseFavoringPublished(destination.releases)
    val sourceEntries = source.vulnerabilities.filter { it.id in vulnIds }
    val existingById = destination.vulnerabilities.associateBy { it.id }
    val preservedStyles = detectBlockScalarStyles(sourceContent) + detectBlockScalarStyles(destinationContent)

    val newContent =
        sourceEntries.fold(destinationContent.content) { acc, incoming ->
            val existing = existingById[incoming.id]
            val merged = mergeVulnerabilityEntry(existing, incoming, release)
            val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(merged), mapper, preservedStyles)
            if (existing == null) {
                insertEntryAfterVulnerabilitiesHeader(acc, entryYaml)
            } else {
                replaceEntryById(acc, incoming.id, entryYaml)
            }
        }
    return CopyOutcome(
        copied = sourceEntries.map { it.id },
        newContent = VulnlogFileRaw(newContent),
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

fun findNonExistingVulnIds(
    vulnerabilities: List<VulnerabilityEntry>,
    vulnIds: Set<VulnId>,
): Set<VulnId> =
    vulnIds
        .filter { vulnId -> vulnId !in vulnerabilities.map(VulnerabilityEntry::id) }
        .toSet()

/** Latest published release, or the last entry when none is published. */
fun lastReleaseFavoringPublished(releases: List<ReleaseEntry>): Release =
    releases.lastOrNull { it.publicationDate != null }?.id ?: releases.last().id

/** Renders an entry as a `vulnerabilities` list item: `- ` on the first line, the rest indented to match. */
fun serializeEntryYaml(
    entry: VulnerabilityEntryDto,
    mapper: ObjectMapper,
    preservedStyles: Map<String, BlockScalarStyle> = emptyMap(),
): String {
    val raw = CanonicalYaml.renderEntry(entry, mapper, preservedStyles)
    val lines =
        raw
            .lines()
            .dropWhile { it.trimStart().startsWith("---") || it.isBlank() }
            .dropLastWhile { it.isBlank() }

    val itemPrefix = " ".repeat(CanonicalYaml.INDENTATION) + "- "
    val continuationPrefix = " ".repeat(CanonicalYaml.INDENTATION + 2)
    return lines
        .mapIndexed { index, line ->
            if (index == 0) "$itemPrefix$line" else "$continuationPrefix$line"
        }.joinToString("\n")
}

/**
 * Inserts [entryYaml] after the `vulnerabilities:` header, rewriting an empty `vulnerabilities: []`
 * placeholder to a real block first. Fails if no such header exists.
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

private val ENTRY_ID_LINE = Regex("""^\s*- id:\s+["']?(\S+?)["']?\s*$""")

/**
 * Replaces the YAML block of the entry whose `id` matches [vulnId] with [newEntryYaml]. When the id is
 * absent, inserts after the `vulnerabilities:` header if [insertIfMissing], otherwise fails fast.
 *
 * The block is delimited by the next `  - id:` line, the next top-level YAML key, or the end of the file.
 * Trailing blank lines that visually separate this entry from the next are preserved as-is.
 */
fun replaceEntryById(
    fileContent: String,
    vulnId: VulnId,
    newEntryYaml: String,
    insertIfMissing: Boolean = true,
): String {
    val lines = fileContent.lines()
    val startIndex =
        lines.indexOfFirst { line ->
            ENTRY_ID_LINE.matchEntire(line)?.groupValues?.get(1) == vulnId.id
        }
    if (startIndex == -1) {
        check(insertIfMissing) { "Vulnerability entry '${vulnId.id}' was parsed but not found in the source text." }
        return insertEntryAfterVulnerabilitiesHeader(fileContent, newEntryYaml)
    }
    var endIndex = lines.size
    for (i in startIndex + 1 until lines.size) {
        val line = lines[i]
        if (ENTRY_ID_LINE.matches(line)) {
            endIndex = i
            break
        }
        // the next top-level key ends the vulnerabilities section
        if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("#") && !line.startsWith("-")) {
            endIndex = i
            break
        }
    }
    // keep trailing blank separators between this entry and the next
    while (endIndex > startIndex + 1 && lines[endIndex - 1].isBlank()) {
        endIndex--
    }

    val rebuilt = lines.subList(0, startIndex) + newEntryYaml.lines() + lines.subList(endIndex, lines.size)
    return rebuilt.joinToString("\n")
}
