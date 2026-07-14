// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

data class SnykIgnoreEntryDto(
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val reason: String? = null,
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val expires: LocalDateTime? = null,
)
