package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VulnlogFileV1Dto(
    val schemaVersion: String,
    val project: ProjectDto,
    val releases: List<ReleaseEntryDto>,
    val vulnerabilities: List<VulnerabilityEntryDto>,
)
