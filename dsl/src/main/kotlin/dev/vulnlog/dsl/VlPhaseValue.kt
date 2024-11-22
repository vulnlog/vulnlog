package dev.vulnlog.dsl

import java.time.LocalDate

interface VlPhaseValue {
    val name: String
    val phaseDuration: Pair<LocalDate, LocalDate>
}
