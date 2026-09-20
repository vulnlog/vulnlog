// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The identity fields of an existing document. Everything else is ignored: a baseline is read for continuity only,
 * never for its content. Every field is nullable so a foreign document binds and is rejected afterwards.
 *
 * Only `@context` is read before the format version is known, and every version places it here. A version that moves
 * an identity field binds its own DTO in the reader's branch for it.
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
