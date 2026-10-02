// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.finding

/** Stops validation: the file cannot be read into the domain model, so no rule can run on it. */
data class ParseFailure(
    val message: String,
    val path: String? = null,
    val location: FailureLocation? = null,
)

/** 1-based line and column in the source text. */
data class FailureLocation(
    val line: Int,
    val column: Int,
)
