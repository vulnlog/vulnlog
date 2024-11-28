package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlPhaseValue
import java.time.LocalDate

data class VlPhaseValueImpl(override val name: String, override val phaseDuration: Pair<LocalDate, LocalDate>) :
    VlPhaseValue
