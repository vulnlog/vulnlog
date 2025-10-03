package dev.vulnlog.common.model

data class ReporterConfig(
    val reporterName: String,
    val templateFilename: String,
    val idMatcher: String,
    val template: String,
)
