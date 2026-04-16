// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.v1.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class VulnlogFileV1Dto(
    val schemaVersion: String,
    val project: ProjectDto,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val tags: List<TagEntryDto>? = null,
    val releases: List<ReleaseEntryDto>,
    val vulnerabilities: List<VulnerabilityEntryDto>,
)
