package dev.vulnlog.cli.parse.v1.dto

data class ReleasePurlEntryDto(
    val purl: String,
    val tags: List<String>,
)
