// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChangelogSummaryDto(
    val total: Int,
    /** The count per severity, most severe first. A severity without fixes is left out. */
    @param:JsonProperty("by_severity")
    val bySeverity: Map<String, Int>,
)
