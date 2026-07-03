// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.validation

/** 1-based line and column in the source text. */
data class FailureLocation(
    val line: Int,
    val column: Int,
)
