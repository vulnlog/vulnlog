package dev.vulnlog.cli.parse.suppression.generic.dto

data class GenericSuppressionDto(
    val vulnerabilities: List<GenericVulnerabilityEntryDto>,
)
