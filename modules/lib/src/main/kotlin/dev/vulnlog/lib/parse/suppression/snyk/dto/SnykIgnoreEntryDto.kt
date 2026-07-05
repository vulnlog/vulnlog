// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class SnykIgnoreEntryDto(
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val reason: String? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val expires: String? = null,
)
