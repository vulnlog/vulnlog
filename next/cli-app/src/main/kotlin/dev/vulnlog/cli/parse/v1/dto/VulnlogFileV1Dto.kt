package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class VulnlogFileV1Dto
    @JsonCreator
    constructor(
        @param:JsonProperty("schemaVersion") val schemaVersion: String,
        @param:JsonProperty("project") val project: ProjectDto,
        @param:JsonProperty("tags") val tags: List<TagEntryDto>? = null,
        @param:JsonProperty("releases") val releases: List<ReleaseEntryDto>,
        @param:JsonProperty("vulnerabilities") val vulnerabilities: List<VulnerabilityEntryDto>,
    )
