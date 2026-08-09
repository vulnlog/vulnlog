// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.dto

data class ReleasePurlEntryDto(
    val purl: String,
    val tags: List<String>,
)
