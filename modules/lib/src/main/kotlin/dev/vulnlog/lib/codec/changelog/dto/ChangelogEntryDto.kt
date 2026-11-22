// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChangelogEntryDto(
    @param:JsonProperty("primary_id")
    val primaryId: String,
    val aliases: List<String>,
    val name: String?,
    val description: String?,
    val verdict: String?,
    val severity: String?,
    /** The justification of an entry that is not affected. */
    val justification: String?,
    /** How the vulnerability was resolved. */
    val note: String?,
    val ref: String?,
)
