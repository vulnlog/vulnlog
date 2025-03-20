package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class InvolvedReleaseVersions(
    val affectedReleaseVersion: InvolvedReleaseVersion?,
    val fixedReleaseVersion: InvolvedReleaseVersion?,
)

@Serializable
data class InvolvedReleaseVersion(val version: String?, val publicationDate: String?)
