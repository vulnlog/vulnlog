// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.core.pluralize
import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.mapper.DtoV1Mapper
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.document.yaml.hasSchemaHeader
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import java.nio.file.Path

data class CopyOutcome(
    val copied: List<VulnId>,
    val newContent: String,
)

/**
 * Rewrites the whole document canonically, as [addVulnerabilityToFile] does. An entry the destination already has
 * keeps its place and its own values and only gains what it lacks, so a copy never overwrites the destination's
 * analysis. Every copied entry points at the destination's last release; the source's releases mean nothing there.
 */
fun copyVulnerabilities(
    source: VulnlogFile,
    destination: ValidVulnlogProject,
    vulnIds: Set<VulnId>,
): CopyOutcome {
    val destinationFile = destination.vulnlogProjectFile
    val release =
        destinationFile.releases
            .lastOrNull()
            ?.id
    val sourceEntries = source.vulnerabilities.filter { it.id in vulnIds }
    val existingById = destinationFile.vulnerabilities.associateBy { it.id }

    val destinationDto = destination.parsedVulnlogProject.validatedDto
    val newDto =
        when (destinationDto) {
            is VulnlogFileV1Dto ->
                sourceEntries.fold(destinationDto) { acc, incoming ->
                    val merged = mergeVulnerabilityEntry(existingById[incoming.id], incoming, release)
                    upsertEntry(acc, incoming.id, DtoV1Mapper.vulnerabilityToDto(merged))
                }
        }

    return CopyOutcome(
        copied = sourceEntries.map { it.id },
        newContent =
            YamlWriter.renderCanonicalDocument(
                newDto,
                includeSchemaHeader = hasSchemaHeader(destination.nodeTree.rootNode),
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
    release: Release?,
): VulnerabilityEntry {
    val releases = release?.let(::listOf) ?: emptyList()
    if (existing == null) return incoming.copy(releases = releases)
    return existing.copy(
        name = existing.name ?: incoming.name,
        aliases = unionPreservingOrder(existing.aliases, incoming.aliases),
        releases = releases,
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
    formatMessage(
        FindingSeverity.ERROR,
        "vulnerability IDs not found in source file: ${missing.joinToString(", ") { it.id }}",
    )

fun formatCopiedMessage(
    destinationPath: Path,
    ids: List<VulnId>,
): String =
    if (ids.isEmpty()) {
        formatStatus(StatusVerb.UNCHANGED, "$destinationPath: no new vulnerabilities")
    } else {
        formatStatus(StatusVerb.COPIED, "${pluralize(ids.size, "entry", "entries")} to $destinationPath")
    }

fun findNonExistingVulnIds(
    vulnerabilities: List<VulnerabilityEntry>,
    vulnIds: Set<VulnId>,
): Set<VulnId> = vulnIds - vulnerabilities.map { it.id }.toSet()
