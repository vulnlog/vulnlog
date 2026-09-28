// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

/**
 * The identity fields and the statements of an existing document. Everything else is ignored: a baseline is read for
 * continuity only. Every field is nullable so a foreign document binds and is rejected afterwards.
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
    val version: Long? = null,
    /**
     * Kept as a tree, because statements are read back one by one: one this writer cannot read is skipped, never
     * failing the baseline.
     */
    val statements: JsonNode? = null,
)
