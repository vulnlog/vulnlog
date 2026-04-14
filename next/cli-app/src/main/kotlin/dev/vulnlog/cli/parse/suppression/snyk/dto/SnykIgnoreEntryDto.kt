// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.suppression.snyk.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

data class SnykIgnoreEntryDto(
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val reason: String? = null,
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val expires: LocalDate? = null,
)
