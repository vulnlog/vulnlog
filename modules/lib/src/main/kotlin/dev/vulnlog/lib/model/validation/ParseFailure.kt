// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.validation

/** One problem found while parsing, with the YAML path and source position when known. */
data class ParseFailure(
    val message: String,
    val path: String? = null,
    val location: FailureLocation? = null,
)
