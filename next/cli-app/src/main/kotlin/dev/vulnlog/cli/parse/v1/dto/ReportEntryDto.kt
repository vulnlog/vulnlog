package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ReportEntryDto(
    val reporter: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    val at: LocalDate? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val source: String? = null,
    @param:JsonProperty("vuln_ids")
    @param:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val vulnIds: Set<String> = emptySet(),
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val suppress: SuppressionDto? = null,
)
