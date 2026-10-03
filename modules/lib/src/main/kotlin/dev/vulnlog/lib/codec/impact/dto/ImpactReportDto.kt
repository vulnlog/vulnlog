// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact.dto

data class ImpactReportDto(
    val project: ProjectDto,
    val generatedAt: String,
    val vulnlogVersion: String,
    val inputs: List<String>,
    val filter: FilterDto,
    val entries: List<ImpactEntryDto>,
)
