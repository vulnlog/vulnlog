package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlPhaseValue
import java.time.LocalDate

data class VlPhaseValueImpl(override val name: String, override val phaseDuration: Pair<LocalDate, LocalDate>) :
    VlPhaseValue
