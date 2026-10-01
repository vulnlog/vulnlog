// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag

sealed interface FilterOutcome {
    data class Resolved(
        val filter: ResolvedFilter,
    ) : FilterOutcome

    data class Rejected(
        val problems: List<FilterProblem>,
    ) : FilterOutcome
}

/**
 * Why one filter dimension could not be resolved. Only release and tag problems carry the `known` values, because only
 * those come from the files; render names the fixed vocabularies of the other dimensions.
 */
sealed interface FilterProblem {
    data class BlankRelease(
        val known: List<Release>,
    ) : FilterProblem

    data class UnknownRelease(
        val requested: Release,
        val known: List<Release>,
    ) : FilterProblem

    data class BlankTag(
        val known: List<Tag>,
    ) : FilterProblem

    data class UnknownTags(
        val requested: List<Tag>,
        val known: List<Tag>,
    ) : FilterProblem

    data class UnknownReporter(
        val requested: String,
    ) : FilterProblem

    data class UnknownStates(
        val requested: List<String>,
    ) : FilterProblem

    data class UnknownVerdicts(
        val requested: List<String>,
    ) : FilterProblem

    data class UnknownDispositions(
        val requested: List<String>,
    ) : FilterProblem
}
