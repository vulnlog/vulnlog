package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlRatingValue
import java.time.LocalDate

internal data class VlRatingNotAffectedValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue {
    override val rating = "not affected"
}
