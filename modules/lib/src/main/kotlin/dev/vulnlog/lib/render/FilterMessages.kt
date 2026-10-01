// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.dispositionTokens
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.verdictKindTokens
import dev.vulnlog.lib.core.workStateTokens
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag

fun renderFilterProblems(problems: List<FilterProblem>): List<Failure> =
    problems.map { problem -> Failure(message(problem), hint(problem)) }

private fun message(problem: FilterProblem): String =
    when (problem) {
        is FilterProblem.BlankRelease -> "Release must not be blank"
        is FilterProblem.UnknownRelease -> "Release not found: ${problem.requested.value}"
        is FilterProblem.BlankTag -> "Tag must not be blank"
        is FilterProblem.UnknownTags -> "Tag not found: ${problem.requested.joinToString(", ") { it.value }}"
        is FilterProblem.UnknownReporter -> "Invalid reporter: ${problem.requested}"
        is FilterProblem.UnknownStates -> "Invalid state: ${problem.requested.joinToString(", ")}"
        is FilterProblem.UnknownVerdicts -> "Invalid verdict: ${problem.requested.joinToString(", ")}"
        is FilterProblem.UnknownDispositions -> "Invalid disposition: ${problem.requested.joinToString(", ")}"
    }

private fun hint(problem: FilterProblem): String =
    when (problem) {
        is FilterProblem.BlankRelease -> knownReleases(problem.known)
        is FilterProblem.UnknownRelease -> knownReleases(problem.known)
        is FilterProblem.BlankTag -> knownTags(problem.known)
        is FilterProblem.UnknownTags -> knownTags(problem.known)
        is FilterProblem.UnknownReporter ->
            "Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}"

        is FilterProblem.UnknownStates -> "Supported states: ${workStateTokens()}"
        is FilterProblem.UnknownVerdicts -> "Supported verdicts: ${verdictKindTokens()}"
        is FilterProblem.UnknownDispositions -> "Supported dispositions: ${dispositionTokens()}"
    }

private fun knownReleases(known: List<Release>): String = "Known releases: ${known.joinToString(", ") { it.value }}"

private fun knownTags(known: List<Tag>): String =
    if (known.isEmpty()) "The input declares no tags." else "Known tags: ${known.joinToString(", ") { it.value }}"
