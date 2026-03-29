package dev.vulnlog.cli.parse.suppression.trivy.dto

data class TrivySuppressionDto(
    val vulnerabilities: List<TrivyVulnerabilityEntryDto>,
)
