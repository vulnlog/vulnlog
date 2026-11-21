// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ImpactReportDto(
    @param:JsonProperty("format_version")
    val formatVersion: Int,
    val project: ProjectDto,
    /** ISO 8601 instant in UTC. */
    @param:JsonProperty("generated_at")
    val generatedAt: String,
    @param:JsonProperty("vulnlog_version")
    val vulnlogVersion: String,
    /** The input file names, in the order given. */
    val inputs: List<String>,
    val filter: FilterDto,
    val entries: List<ImpactEntryDto>,
)
