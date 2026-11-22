// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog

import dev.vulnlog.lib.codec.changelog.dto.ChangelogEntryDto
import dev.vulnlog.lib.codec.changelog.dto.ChangelogReleaseDto
import dev.vulnlog.lib.codec.changelog.dto.ChangelogReportDto
import dev.vulnlog.lib.codec.changelog.dto.ChangelogSummaryDto
import dev.vulnlog.lib.codec.changelog.dto.FilterDto
import dev.vulnlog.lib.codec.report.formatGeneratedAt
import dev.vulnlog.lib.codec.report.labelJustification
import dev.vulnlog.lib.codec.report.labelSeverity
import dev.vulnlog.lib.codec.report.labelVerdict
import dev.vulnlog.lib.codec.report.sortTagValues
import dev.vulnlog.lib.codec.report.toProjectDto
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.reporting.ChangelogReport
import dev.vulnlog.lib.model.reporting.ChangelogEntry
import dev.vulnlog.lib.model.reporting.ChangelogRelease
import dev.vulnlog.lib.model.reporting.ChangelogSummary

private const val FORMAT_VERSION = 1

internal object ChangelogMapper {
    fun toDto(report: ChangelogReport): ChangelogReportDto =
        ChangelogReportDto(
            formatVersion = FORMAT_VERSION,
            project = toProjectDto(report.changelog.project),
            generatedAt = formatGeneratedAt(report.generatedAt),
            vulnlogVersion = report.vulnlogVersion,
            inputs = report.inputs,
            filter = toFilterDto(report),
            releases = report.changelog.releases.map(::toReleaseDto),
        )

    private fun toFilterDto(report: ChangelogReport): FilterDto =
        FilterDto(
            asOf = report.asOf?.value,
            tags = sortTagValues(report.filter),
            reporter = report.filter.reporter?.canonical(),
            fixedIn = report.filter.fixedIn?.value,
        )

    private fun toReleaseDto(release: ChangelogRelease): ChangelogReleaseDto =
        ChangelogReleaseDto(
            release = release.fixedIn.value,
            publishedAt = release.publishedAt?.toString(),
            summary = toSummaryDto(release.summary),
            entries = release.entries.map(::toEntryDto),
        )

    private fun toSummaryDto(summary: ChangelogSummary): ChangelogSummaryDto =
        ChangelogSummaryDto(
            total = summary.total,
            bySeverity = summary.bySeverity.entries.associate { (severity, count) -> canonical(severity) to count },
        )

    private fun toEntryDto(entry: ChangelogEntry): ChangelogEntryDto =
        ChangelogEntryDto(
            primaryId = entry.primaryId.id,
            aliases = entry.aliases.map { it.id },
            name = entry.name,
            description = entry.description,
            verdict = labelVerdict(entry.impact),
            severity = labelSeverity(entry.impact),
            justification = labelJustification(entry.impact),
            note = entry.note,
            ref = entry.ref,
        )
}
