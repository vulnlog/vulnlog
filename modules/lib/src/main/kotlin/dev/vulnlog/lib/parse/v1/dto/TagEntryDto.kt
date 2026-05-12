// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.v1.dto

data class TagEntryDto(
    val id: String,
    val description: String? = null,
)
