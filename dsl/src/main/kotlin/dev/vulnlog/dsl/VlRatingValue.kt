package dev.vulnlog.dsl

import java.time.LocalDate

interface VlRatingValue {
    val rating: String
    val dateOfAnalysing: LocalDate
    val reasoning: String
}
