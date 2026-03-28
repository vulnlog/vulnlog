package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ResolutionDto(
    @param:JsonProperty("in")
    val release: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val at: LocalDate? = null,
    val ref: String? = null,
    val note: String? = null,
)
