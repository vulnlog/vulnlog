package dev.vulnlog.cli.model

data class TagEntry(
    /**
     * Unique identifier for the tag. Referenced by vulnerability entries and release purl entries.
     */
    val id: Tag,
    /**
     * Human-readable description of what this tag represents.
     */
    val description: String? = null,
)
