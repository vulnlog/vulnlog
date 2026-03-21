package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ProjectDto
    @JsonCreator
    constructor(
        @param:JsonProperty("organization") val organization: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("author") val author: String,
        @param:JsonProperty("contact") val contact: String? = null,
    )
