// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ReleaseEntryDto(
    val id: String,
    @param:JsonProperty("published_at")
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val publishedAt: LocalDate? = null,
    val purls: List<ReleasePurlEntryDto>? = null,
)
