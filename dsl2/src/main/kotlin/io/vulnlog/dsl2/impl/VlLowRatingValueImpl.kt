package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlRatingValue
import java.time.LocalDate

data class VlLowRatingValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue
