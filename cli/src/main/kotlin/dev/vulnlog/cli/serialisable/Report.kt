package dev.vulnlog.cli.serialisable

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Report(
    val analyser: String,
    @Serializable(with = LocalDateSerialiser::class)
    val awareAt: LocalDate,
)
