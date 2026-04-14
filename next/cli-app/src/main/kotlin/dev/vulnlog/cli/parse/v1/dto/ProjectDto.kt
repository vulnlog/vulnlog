// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class ProjectDto(
    val organization: String,
    val name: String,
    val author: String,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val contact: String? = null,
)
