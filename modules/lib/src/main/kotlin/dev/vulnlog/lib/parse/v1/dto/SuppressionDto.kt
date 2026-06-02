// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class SuppressionDto(
    @param:JsonProperty("expires_at")
    @param:JsonFormat(pattern = "yyyy-MM-dd")
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val expiresAt: LocalDate? = null,
)
