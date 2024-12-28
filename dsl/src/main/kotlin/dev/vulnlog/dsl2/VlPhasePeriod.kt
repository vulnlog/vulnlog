package dev.vulnlog.dsl2

import java.time.LocalDate
import java.time.Month
import java.time.Period

data class VlPhasePeriod(val start: LocalDate, val end: LocalDate) {
    fun months(): Int {
        val periode = Period.between(start, end)
        return periode.months + periode.years * Month.entries.size
    }
}
