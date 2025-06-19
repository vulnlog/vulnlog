package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val action: String,
    val details: List<String> = emptyList(),
)
