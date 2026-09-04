// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * One serialized statement. `justification` is required for `not_affected` and `action_statement` for `affected`;
 * both are absent for every other status.
 */
data class OpenVexStatementDto(
    val vulnerability: OpenVexVulnerabilityDto,
    val products: List<OpenVexProductDto>,
    val status: String,
    val justification: String? = null,
    @param:JsonProperty("action_statement")
    val actionStatement: String? = null,
)
