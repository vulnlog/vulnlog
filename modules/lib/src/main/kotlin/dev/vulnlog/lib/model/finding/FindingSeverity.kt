// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.finding

enum class FindingSeverity {
    /**
     * Observations that help the user improve their file.
     */
    INFO,

    /**
     * File is technically valid, but something is likely wrong or will cause problems in the future.
     */
    WARNING,

    /**
     * Indicate structurally invalid or semantically broken files that would produce incorrect output.
     */
    ERROR,
}

/** Helper for filtering for specific finding severity levels */
val ERRORS_ONLY: Set<FindingSeverity> = setOf(FindingSeverity.ERROR)
val ALL_SEVERITIES: Set<FindingSeverity> = FindingSeverity.entries.toSet()
