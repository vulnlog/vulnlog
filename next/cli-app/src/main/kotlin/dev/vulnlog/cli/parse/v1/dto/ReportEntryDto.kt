package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ReportEntryDto
    @JsonCreator
    constructor(
        @param:JsonProperty("reporter") val reporter: String,
        @param:JsonProperty("at")
        @param:JsonFormat(pattern = "yyyy-MM-dd")
        val at: LocalDate? = null,
    )
