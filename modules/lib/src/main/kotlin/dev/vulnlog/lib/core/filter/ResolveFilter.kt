// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Checks [request] against every file it will be applied to and expands the release window.
 *
 * A release must be defined in every file, because the window is read from each file's own release
 * order. A tag only has to be defined in one of them, because a tag filter is set membership rather
 * than a point on a timeline.
 */
fun resolveFilter(
    request: FilterRequest,
    files: List<VulnlogFile>,
): FilterOutcome {
    val releases = resolveReleaseWindow(request.release, files)
    val tags = resolveTags(request.tags, files)
    val problems = releases.problems + tags.problems

    return if (problems.isEmpty()) {
        FilterOutcome.Resolved(ResolvedFilter(request.reporter, releases.value, tags.value))
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

private fun resolveReleaseWindow(
    release: Release?,
    files: List<VulnlogFile>,
): Dimension<Set<Release>> {
    if (release == null) return Dimension(emptySet())

    val known = files.flatMap { file -> file.releases.map { it.id } }.distinct()
    if (files.any { file -> file.releases.none { it.id == release } }) {
        return Dimension(
            emptySet(),
            listOf(
                FilterProblem(
                    "Release not found: ${release.value}",
                    "Known releases: ${known.joinToString(", ") { it.value }}",
                ),
            ),
        )
    }
    return Dimension(files.flatMap { file -> windowOf(release, file) }.toSet())
}

/** The requested release and every release declared before it in [file]. */
private fun windowOf(
    release: Release,
    file: VulnlogFile,
): List<Release> {
    val ordered = file.releases.map { it.id }
    return ordered.take(ordered.indexOf(release) + 1)
}

private fun resolveTags(
    tags: Set<Tag>,
    files: List<VulnlogFile>,
): Dimension<Set<Tag>> {
    if (tags.isEmpty()) return Dimension(emptySet())

    val known = files.flatMap { file -> file.tags.map { it.id } }.distinct()
    val unknown = tags.filterNot { it in known }.sortedBy { it.value }
    return if (unknown.isEmpty()) {
        Dimension(tags)
    } else {
        Dimension(
            emptySet(),
            listOf(
                FilterProblem(
                    "Tag not found: ${unknown.joinToString(", ") { it.value }}",
                    if (known.isEmpty()) {
                        "The input declares no tags."
                    } else {
                        "Known tags: ${known.joinToString(", ") { it.value }}"
                    },
                ),
            ),
        )
    }
}
