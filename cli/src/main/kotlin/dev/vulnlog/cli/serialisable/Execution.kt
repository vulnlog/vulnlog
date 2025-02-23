package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable

@Serializable
data class Execution(val action: String, val duration: String)
