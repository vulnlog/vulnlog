package dev.vulnlog.report.serialisable

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Analysis(
    @Serializable(with = LocalDateSerialiser::class)
    val analysedAt: LocalDate,
    val verdict: String,
    val reasoning: String,
)
