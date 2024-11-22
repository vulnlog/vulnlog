package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlRatingValue
import java.time.LocalDate

internal data class VlRatingLowValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue {
    override val rating = "low"
}
