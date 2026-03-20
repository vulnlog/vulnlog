package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class ReportEntryDto(
    val reporter: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val at: LocalDate? = null,
)
