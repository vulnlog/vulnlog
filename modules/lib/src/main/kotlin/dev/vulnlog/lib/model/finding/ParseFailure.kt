// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.finding

/** One problem found while reading a file, with the YAML path and source position when known. */
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
