package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class VulnIdentifier(
    val id: String,
    val groupIds: List<String>,
)
