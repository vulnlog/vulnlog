package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class TagEntryDto
    @JsonCreator
    constructor(
        @param:JsonProperty("id") val id: String,
        @param:JsonProperty("description") val description: String? = null,
    )
