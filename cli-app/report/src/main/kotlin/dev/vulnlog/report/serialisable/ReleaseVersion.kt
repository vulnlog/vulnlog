package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ReleaseVersion(
    val version: String,
    @Serializable(with = LocalDateSerialiser::class)
    val publicationDate: LocalDate?,
)
