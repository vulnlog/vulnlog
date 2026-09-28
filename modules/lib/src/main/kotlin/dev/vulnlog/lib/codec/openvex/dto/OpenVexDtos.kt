// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode

// Property order is the emitted key order. Every DTO here is listed in the native-image reflect-config.json.

data class OpenVexDocumentDto(
    @param:JsonProperty("@context")
    val context: String,
    @param:JsonProperty("@id")
    val id: String,
    val author: String,
    val role: String,
    val timestamp: String,
    val version: Int,
    val tooling: String? = null,
    val statements: List<OpenVexStatementDto>,
)

data class OpenVexStatementDto(
    val vulnerability: OpenVexVulnerabilityDto,
    val timestamp: String,
    val products: List<OpenVexProductDto>,
    val status: String,
    val justification: String? = null,
    @param:JsonProperty("impact_statement")
    val impactStatement: String? = null,
    @param:JsonProperty("action_statement")
    val actionStatement: String? = null,
    @param:JsonProperty("action_statement_timestamp")
    val actionStatementTimestamp: String? = null,
    @param:JsonProperty("status_notes")
    val statusNotes: String? = null,
    val supplier: String,
)

data class OpenVexVulnerabilityDto(
    @param:JsonProperty("@id")
    val id: String,
    val name: String,
    val description: String? = null,
    val aliases: List<String>? = null,
)

data class OpenVexProductDto(
    @param:JsonProperty("@id")
    val id: String,
    val identifiers: OpenVexIdentifiersDto,
    val subcomponents: List<OpenVexSubcomponentDto>? = null,
)

data class OpenVexIdentifiersDto(
    val purl: String,
)

data class OpenVexSubcomponentDto(
    @param:JsonProperty("@id")
    val id: String,
    val identifiers: OpenVexIdentifiersDto,
)

/**
 * Every field is nullable, so a foreign document binds and is rejected afterwards. The statements stay a tree, because
 * each one is read back leniently on its own.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenVexBaselineDto(
    @param:JsonProperty("@context")
    val context: String? = null,
    @param:JsonProperty("@id")
    val id: String? = null,
    val timestamp: String? = null,
    val version: Long? = null,
    val statements: JsonNode? = null,
)
