package dev.vulnlog.dsl

public data class VlReporterConfig(
    public val templateFilename: String,
    public val idMatcher: String,
    public val template: String,
)
