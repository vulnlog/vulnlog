// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** The OpenVEX document as it is serialized. Property order is the emitted key order. */
data class OpenVexDocumentDto(
    @param:JsonProperty("@context")
    val context: String,
    @param:JsonProperty("@id")
    val id: String,
    val author: String,
    val timestamp: String,
    val version: Int,
    val statements: List<OpenVexStatementDto>,
)
