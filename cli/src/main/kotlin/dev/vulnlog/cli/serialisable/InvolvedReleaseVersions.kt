package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class InvolvedReleaseVersions(
    val affectedReleaseVersion: InvolvedReleaseVersion?,
    val fixedReleaseVersion: InvolvedReleaseVersion?,
)

@Serializable
data class InvolvedReleaseVersion(
    val version: String?,
    @Serializable(with = LocalDateSerialiser::class)
    val publicationDate: LocalDate?,
)
