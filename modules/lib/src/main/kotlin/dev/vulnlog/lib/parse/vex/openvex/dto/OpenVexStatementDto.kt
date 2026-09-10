// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * One serialized statement. `justification` and `impact_statement` belong to `not_affected`; `action_statement`, its
 * timestamp and `status_notes` to the other statuses. Every statement is dated, so none inherits from the document.
 */
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
