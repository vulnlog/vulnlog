// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

/** What the caller asked to filter [dev.vulnlog.lib.model.VulnerabilityEntry] on, before any check against a file. */
data class FilterRequest(
    val reporter: String? = null,
    val asOf: String? = null,
    val tags: Set<String> = emptySet(),
    val states: Set<String> = emptySet(),
    val verdicts: Set<String> = emptySet(),
)
