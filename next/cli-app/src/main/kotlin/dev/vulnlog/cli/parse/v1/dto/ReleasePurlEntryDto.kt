package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ReleasePurlEntryDto
    @JsonCreator
    constructor(
        @param:JsonProperty("purl") val purl: String,
        @param:JsonProperty("tags") val tags: List<TagEntryDto>,
    )
