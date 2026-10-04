// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.WorkState

/** Resolves every dimension before deciding, so a rejection names all problems at once. */
fun resolveFilter(
    request: FilterRequest,
    files: List<VulnlogFile>,
): FilterOutcome {
    val reporter = resolveReporter(request.reporter)
    val releases = resolveReleaseWindow(request.asOf, files)
    val tags = resolveTags(request.tags, files)
    val states = resolveStates(request.states)
    val verdicts = resolveVerdicts(request.verdicts)
    val dispositions = resolveDispositions(request.dispositions)
    val fixedIn = resolveRelease(request.fixedIn, files)
    val problems =
        reporter.problems + releases.problems + tags.problems + states.problems + verdicts.problems +
            dispositions.problems + fixedIn.problems

    return if (problems.isEmpty()) {
        FilterOutcome.Resolved(
            ResolvedFilter(
                reporter = reporter.value,
                releases = releases.value,
                tags = tags.value,
                states = states.value,
                verdicts = verdicts.value,
                dispositions = dispositions.value,
                fixedIn = fixedIn.value,
            ),
        )
    } else {
        FilterOutcome.Rejected(problems)
    }
}

/** One resolved dimension: the value to filter with, or the problems that stopped it resolving. */
internal data class Dimension<T>(
    val value: T,
    val problems: List<FilterProblem> = emptyList(),
)

private fun resolveReporter(value: String?): Dimension<ReporterType?> {
    if (value == null) return Dimension(null)

    return try {
        Dimension(parseReporter(value))
    } catch (_: IllegalArgumentException) {
        Dimension(null, listOf(FilterProblem.UnknownReporter(value)))
    }
}

private fun resolveReleaseWindow(
    value: String?,
    files: List<VulnlogFile>,
): Dimension<Set<Release>> {
    if (value == null) return Dimension(emptySet())

    val known =
        files
            .map { file -> file.releases.map { it.id } }
            .reduceOrNull { common, ids -> common.filter { it in ids } }
            .orEmpty()
            .distinct()
    if (value.isBlank()) return Dimension(emptySet(), listOf(FilterProblem.BlankRelease(known)))

    val release = Release(value)
    if (release !in known) return Dimension(emptySet(), listOf(FilterProblem.UnknownRelease(release, known)))

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

private fun resolveStates(values: Set<String>): Dimension<Set<WorkState>> {
    if (values.isEmpty()) return Dimension(emptySet())

    val byToken = WorkState.entries.associateBy { it.canonical() }
    val unknown = values.filterNot { it in byToken }.sorted()
    if (unknown.isNotEmpty()) return Dimension(emptySet(), listOf(FilterProblem.UnknownStates(unknown)))

    return Dimension(values.mapNotNull { byToken[it] }.toSet())
}

private fun resolveVerdicts(values: Set<String>): Dimension<Set<VerdictKind>> {
    if (values.isEmpty()) return Dimension(emptySet())

    val byToken = VerdictKind.entries.associateBy { it.canonical() }
    val unknown = values.filterNot { it in byToken }.sorted()
    if (unknown.isNotEmpty()) return Dimension(emptySet(), listOf(FilterProblem.UnknownVerdicts(unknown)))

    return Dimension(values.mapNotNull { byToken[it] }.toSet())
}

private fun resolveDispositions(values: Set<String>): Dimension<Set<Disposition>> {
    if (values.isEmpty()) return Dimension(emptySet())

    val byToken = Disposition.entries.associateBy { canonical(it) }
    val unknown = values.filterNot { it in byToken }.sorted()
    if (unknown.isNotEmpty()) return Dimension(emptySet(), listOf(FilterProblem.UnknownDispositions(unknown)))

    return Dimension(values.mapNotNull { byToken[it] }.toSet())
}

/** Unlike the release window, one file declaring the release is enough. */
internal fun resolveRelease(
    releaseId: String?,
    files: List<VulnlogFile>,
): Dimension<Release?> {
    if (releaseId == null) return Dimension(null)

    val known: List<Release> = files.flatMap { file -> file.releases.map { it.id } }.distinct()
    if (releaseId.isBlank()) return Dimension(null, listOf(FilterProblem.BlankRelease(known)))

    val release = Release(releaseId)
    if (release !in known) return Dimension(null, listOf(FilterProblem.UnknownRelease(release, known)))

    return Dimension(release)
}

internal fun resolveTags(
    values: Set<String>,
    files: List<VulnlogFile>,
): Dimension<Set<Tag>> {
    if (values.isEmpty()) return Dimension(emptySet())

    val known = files.flatMap { file -> file.tags.map { it.id } }.distinct()
    if (values.any { it.isBlank() }) return Dimension(emptySet(), listOf(FilterProblem.BlankTag(known)))

    val tags = values.map(::Tag).toSet()
    val unknown = tags.filterNot { it in known }.sortedBy { it.value }
    if (unknown.isNotEmpty()) return Dimension(emptySet(), listOf(FilterProblem.UnknownTags(unknown, known)))

    return Dimension(tags)
}
