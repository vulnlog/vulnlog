package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlRatingValue
import java.time.LocalDate

internal data class VlRatingModerateValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue {
    override val rating = "moderate"
}
