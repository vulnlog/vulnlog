package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlRatingValue
import java.time.LocalDate

internal data class VlRatingNotAffectedValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue {
    override val rating = "not affected"
}
