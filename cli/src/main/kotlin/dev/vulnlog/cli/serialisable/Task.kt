package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val action: String,
    val details: List<String> = emptyList(),
)
