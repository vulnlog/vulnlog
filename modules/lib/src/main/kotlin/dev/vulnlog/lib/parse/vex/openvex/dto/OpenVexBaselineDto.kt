// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The identity fields of an existing document. Everything else is ignored: a baseline is read for continuity only,
 * never for its content. Every field is nullable so a foreign document binds and is rejected afterwards.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenVexBaselineDto(
    @param:JsonProperty("@context")
    val context: String? = null,
    @param:JsonProperty("@id")
    val id: String? = null,
    val timestamp: String? = null,
    val version: Int? = null,
)
