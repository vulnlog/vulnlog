// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact.dto

data class FilterDto(
    val asOf: String?,
    val tags: List<String>,
    val reporter: String?,
    val states: List<String>,
    val verdicts: List<String>,
    val dispositions: List<String>,
)
