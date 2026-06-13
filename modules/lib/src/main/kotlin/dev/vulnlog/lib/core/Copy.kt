// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.nio.file.Path

data class CopyOutcome(
    val copied: List<VulnId>,
    val newContent: VulnlogFileRaw,
)

/**
 * Copies the requested vulnerability entries from [source] into [destinationContent] and rewrites the
 * whole document in the canonical style ([YamlWriter.renderCanonicalDocument]), even when nothing is
 * copied. Any valid layout is accepted. The optional `# $schema:` header is kept only when
 * [destinationContent] already had it; YAML comments in [destinationContent] do not survive.
 *
 * If an entry already exists in [destination], it is merged with the source entry and keeps its
 * position: existing values win for scalars; lists (aliases, packages, tags) are unioned; reports are
 * unioned by reporter. New entries are placed at the top of the `vulnerabilities:` list. Releases on
 * every copied entry are rewritten to the destination's latest release.
 */
fun copyVulnerabilities(
    source: VulnlogFile,
    destination: VulnlogFile,
    destinationContent: VulnlogFileRaw,
    vulnIds: Set<VulnId>,
    mapper: ObjectMapper = createYamlMapper(),
): CopyOutcome {
    val release = destination.releases.last().id
    val sourceEntries = source.vulnerabilities.filter { it.id in vulnIds }
    val existingById = destination.vulnerabilities.associateBy { it.id }

    val dto = mapper.readValue<VulnlogFileV1Dto>(destinationContent.content)
    val newDto =
        sourceEntries.fold(dto) { acc, incoming ->
            val merged = mergeVulnerabilityEntry(existingById[incoming.id], incoming, release)
            upsertEntry(acc, incoming.id, V1Mapper.vulnerabilityToDto(merged))
        }
    return CopyOutcome(
        copied = sourceEntries.map { it.id },
        newContent =
            VulnlogFileRaw(
                YamlWriter.renderCanonicalDocument(
                    newDto,
                    mapper,
                    includeSchemaHeader = hasSchemaHeader(destinationContent),
                ),
            ),
    )
}

private fun upsertEntry(
    dto: VulnlogFileV1Dto,
    id: VulnId,
    entry: VulnerabilityEntryDto,
): VulnlogFileV1Dto {
    val index = dto.vulnerabilities.indexOfFirst { parseVulnId(it.id) == id }
    val entries =
        if (index == -1) {
            listOf(entry) + dto.vulnerabilities
        } else {
            dto.vulnerabilities.toMutableList().also { it[index] = entry }
        }
    return dto.copy(vulnerabilities = entries)
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
