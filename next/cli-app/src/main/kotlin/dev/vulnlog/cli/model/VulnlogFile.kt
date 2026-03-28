package dev.vulnlog.cli.model

data class VulnlogFile(
    /**
     * Schema version of the Vulnlog file.
     */
    val schemaVersion: SchemaVersion,
    /**
     * Project metadata.
     */
    val project: Project,
    /**
     * Tag definitions,
     */
    val tags: List<TagEntry> = emptyList(),
    /**
     * Release definitions.
     */
    val releases: List<ReleaseEntry>,
    /**
     * Vulnerability definitions.
     */
    val vulnerabilities: List<VulnerabilityEntry>,
)
