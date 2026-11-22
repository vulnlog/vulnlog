// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog.dto

import com.fasterxml.jackson.annotation.JsonProperty
import dev.vulnlog.lib.codec.report.dto.ProjectDto

data class ChangelogReportDto(
    @param:JsonProperty("format_version")
    val formatVersion: Int,
    val project: ProjectDto,
    /** ISO 8601 instant in UTC, to the second. */
    @param:JsonProperty("generated_at")
    val generatedAt: String,
    @param:JsonProperty("vulnlog_version")
    val vulnlogVersion: String,
    /** The input file names, in the order given. */
    val inputs: List<String>,
    val filter: FilterDto,
    /** Newest release first. */
    val releases: List<ChangelogReleaseDto>,
)
