package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlPhaseValue
import java.time.LocalDate

data class VlPhaseValueImpl(override val name: String, override val phaseDuration: Pair<LocalDate, LocalDate>) :
    VlPhaseValue
