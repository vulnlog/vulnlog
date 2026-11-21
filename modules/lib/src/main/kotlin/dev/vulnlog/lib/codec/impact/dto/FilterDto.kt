// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** The filter as applied, in canonical tokens. An empty list or null means the filter was not set. */
data class FilterDto(
    @param:JsonProperty("as_of")
    val asOf: String?,
    val tags: List<String>,
    val reporter: String?,
    val states: List<String>,
    val verdicts: List<String>,
    val dispositions: List<String>,
)
