package dev.vulnlog.cli.model

data class VulnlogFile(
    val schemaVersion: SchemaVersion,
    val project: Project,
    val tags: List<TagEntry> = emptyList(),
    val releases: List<ReleaseEntry>,
    val vulnerabilities: List<VulnerabilityEntry>,
)
