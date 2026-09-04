// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class ReleasePurlEntryDto(
    val purl: String,
    // Optional in the schema, and omitted rather than written empty: the schema requires at least one tag when the key is present.
    @param:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val tags: List<String> = emptyList(),
)
