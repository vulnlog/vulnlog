// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.v1.dto

data class TagEntryDto(
    val id: String,
    val description: String? = null,
)
