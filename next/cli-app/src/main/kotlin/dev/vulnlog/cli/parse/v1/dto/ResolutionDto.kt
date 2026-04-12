package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ResolutionDto(
    @param:JsonProperty("in")
    val release: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val at: LocalDate? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val ref: String? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val note: String? = null,
)
