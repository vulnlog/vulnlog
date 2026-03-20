package dev.vulnlog.cli.model

import java.time.LocalDate

data class ReleaseEntry(
    val id: Release,
    val description: String? = null,
    val publicationDate: LocalDate? = null,
)
