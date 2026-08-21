// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

/** The result of checking a [FilterRequest] against the Vulnlog files it will be applied to. */
sealed interface FilterOutcome {
    /** Every requested dimension exists in the files. */
    data class Resolved(
        val filter: ResolvedFilter,
    ) : FilterOutcome

    /** At least one requested dimension names something the files do not define. */
    data class Rejected(
        val problems: List<FilterProblem>,
    ) : FilterOutcome
}

/** Why one filter dimension could not be resolved, and what the caller can choose instead. */
data class FilterProblem(
    val message: String,
    val hint: String,
)
