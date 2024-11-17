package io.vulnlog.dsl2

import java.time.LocalDate

interface VlPhaseValue {
    val name: String
    val phaseDuration: Pair<LocalDate, LocalDate>
}
