package dev.vulnlog.common.model

import dev.vulnlog.dsl.VlVerdict
import java.time.LocalDate

data class VulnlogAnalysisData(
    val analysedAt: LocalDate,
    val verdict: VlVerdict,
    val reasoning: String,
)
