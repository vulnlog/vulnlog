// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ImpactEntryDto(
    @param:JsonProperty("primary_id")
    val primaryId: String,
    val aliases: List<String>,
    val state: String,
    val verdict: String?,
    val severity: String?,
    val disposition: String?,
    /** The justification of an entry that is not affected. */
    val justification: String?,
    val description: String?,
    val analysis: String?,
    val releases: List<String>,
    @param:JsonProperty("fixed_in")
    val fixedIn: List<String>,
)
