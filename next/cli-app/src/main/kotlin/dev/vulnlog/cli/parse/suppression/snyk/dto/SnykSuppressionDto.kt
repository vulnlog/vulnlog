package dev.vulnlog.cli.parse.suppression.snyk.dto

data class SnykSuppressionDto(
    val ignore: Map<String, List<Map<String, SnykIgnoreEntryDto>>>,
)
