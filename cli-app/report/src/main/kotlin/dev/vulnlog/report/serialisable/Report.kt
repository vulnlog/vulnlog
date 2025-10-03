package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Report(
    val name: String,
    @Serializable(with = LocalDateSerialiser::class)
    val awareAt: LocalDate,
)
