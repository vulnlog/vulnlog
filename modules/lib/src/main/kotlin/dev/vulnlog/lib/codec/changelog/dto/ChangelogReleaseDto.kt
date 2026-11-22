// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ChangelogReleaseDto(
    val release: String,
    /** ISO 8601 date, or null while the release is not published. */
    @param:JsonProperty("published_at")
    val publishedAt: String?,
    val summary: ChangelogSummaryDto,
    /** Most severe first. */
    val entries: List<ChangelogEntryDto>,
)
