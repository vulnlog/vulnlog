// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.reporting.dto

data class FilterDataDto(
    val release: String?,
    val tags: List<String>,
    val reporter: String?,
)
