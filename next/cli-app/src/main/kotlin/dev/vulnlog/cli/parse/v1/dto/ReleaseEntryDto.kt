package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ReleaseEntryDto
    @JsonCreator
    constructor(
        @param:JsonProperty("id") val id: String,
        @param:JsonProperty("description") val description: String? = null,
        @param:JsonProperty("published_at")
        @param:JsonFormat(pattern = "yyyy-MM-dd")
        val publishedAt: LocalDate? = null,
        @param:JsonProperty("purls") val purls: List<ReleasePurlEntryDto>? = null,
    )
