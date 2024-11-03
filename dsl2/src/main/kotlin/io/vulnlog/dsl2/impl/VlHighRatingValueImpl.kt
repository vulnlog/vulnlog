package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlRatingValue
import java.time.LocalDate

data class VlHighRatingValueImpl(
    override val dateOfAnalysing: LocalDate,
    override val reasoning: String,
) : VlRatingValue
