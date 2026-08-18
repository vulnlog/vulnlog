// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Checks [request] against every file it will be applied to and expands the release window.
 *
 * A release must be defined in every file, because the window is read from each file's own release
 * order. A tag only has to be defined in one of them, because a tag filter is set membership rather
 * than a point on a timeline. A reporter is checked against the reporters Vulnlog supports.
 */
fun resolveFilter(
    request: FilterRequest,
    files: List<VulnlogFile>,
): FilterOutcome {
    val reporter = resolveReporter(request.reporter)
    val releases = resolveReleaseWindow(request.release, files)
    val tags = resolveTags(request.tags, files)
    val problems = reporter.problems + releases.problems + tags.problems

    return if (problems.isEmpty()) {
        FilterOutcome.Resolved(ResolvedFilter(reporter.value, releases.value, tags.value))
    } else {
        FilterOutcome.Rejected(problems)
    }
}

/**
 * Renders one diagnostic line per active filter dimension, stating what the filter resolved to.
 * Inactive dimensions produce no line.
 */
fun renderFilterResolution(filter: ResolvedFilter): List<String> =
    listOfNotNull(
        filter.releases
            .takeIf { it.isNotEmpty() }
            ?.let { releases -> "release filter expanded to releases: ${releases.joinToString(", ") { it.value }}" },
        filter.tags
            .takeIf { it.isNotEmpty() }
            ?.let { tags -> "tag filter matched tags: ${tags.joinToString(", ") { it.value }}" },
        filter.reporter?.let { reporter -> "reporter filter: ${reporter.canonical()}" },
    )

/** One resolved dimension: the value to filter with, or the problems that stopped it resolving. */
private data class Dimension<T>(
    val value: T,
    val problems: List<FilterProblem> = emptyList(),
)

private fun resolveReporter(value: String?): Dimension<ReporterType?> {
    if (value == null) return Dimension(null)

    return try {
        Dimension(parseReporter(value))
    } catch (_: IllegalArgumentException) {
        Dimension(
            null,
            listOf(
                FilterProblem(
                    "Invalid reporter: $value",
                    "Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}",
                ),
            ),
        )
    }
}

private fun resolveReleaseWindow(
    value: String?,
    files: List<VulnlogFile>,
): Dimension<Set<Release>> {
    if (value == null) return Dimension(emptySet())

    val known = files.flatMap { file -> file.releases.map { it.id } }.distinct()
    if (value.isBlank()) return Dimension(emptySet(), listOf(releaseProblem("Release must not be blank", known)))

    val release = Release(value)
    if (files.any { file -> file.releases.none { it.id == release } }) {
        return Dimension(emptySet(), listOf(releaseProblem("Release not found: $value", known)))
    }
    return Dimension(files.flatMap { file -> windowOf(release, file) }.toSet())
}

private fun releaseProblem(
    message: String,
    known: List<Release>,
): FilterProblem = FilterProblem(message, "Known releases: ${known.joinToString(", ") { it.value }}")

/** The requested release and every release declared before it in [file]. */
private fun windowOf(
    release: Release,
    file: VulnlogFile,
): List<Release> {
    val ordered = file.releases.map { it.id }
    return ordered.take(ordered.indexOf(release) + 1)
}

private fun resolveTags(
    values: Set<String>,
    files: List<VulnlogFile>,
): Dimension<Set<Tag>> {
    if (values.isEmpty()) return Dimension(emptySet())

    val known = files.flatMap { file -> file.tags.map { it.id } }.distinct()
    if (values.any { it.isBlank() }) return Dimension(emptySet(), listOf(tagProblem("Tag must not be blank", known)))

    val tags = values.map(::Tag).toSet()
    val unknown = tags.filterNot { it in known }.sortedBy { it.value }
    if (unknown.isNotEmpty()) {
        return Dimension(
            emptySet(),
            listOf(tagProblem("Tag not found: ${unknown.joinToString(", ") { it.value }}", known)),
        )
    }
    return Dimension(tags)
}

private fun tagProblem(
    message: String,
    known: List<Tag>,
): FilterProblem =
    FilterProblem(
        message,
        if (known.isEmpty()) "The input declares no tags." else "Known tags: ${known.joinToString(", ") { it.value }}",
    )
