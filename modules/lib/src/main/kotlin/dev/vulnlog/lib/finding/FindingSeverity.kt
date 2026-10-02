// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.finding

// Declared from least to most severe: comparisons rely on the order.
enum class FindingSeverity {
    /** An observation that could improve the file. */
    INFO,

    /** The file is valid, but something is likely wrong or will cause problems later. */
    WARNING,

    /** The file is structurally invalid or semantically broken; output built from it would be wrong. */
    ERROR,
}

val ERRORS_ONLY: Set<FindingSeverity> = setOf(FindingSeverity.ERROR)
val ALL_SEVERITIES: Set<FindingSeverity> = FindingSeverity.entries.toSet()
