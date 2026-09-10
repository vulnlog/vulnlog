// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** A product a statement applies to, identified by its Package URL, with the vulnerable packages inside it. */
data class OpenVexProductDto(
    @param:JsonProperty("@id")
    val id: String,
    val identifiers: OpenVexIdentifiersDto,
    val subcomponents: List<OpenVexSubcomponentDto>? = null,
)

/** The identifiers of a product. Only the Package URL is written. */
data class OpenVexIdentifiersDto(
    val purl: String,
)

/** A vulnerable package inside a product, identified by its Package URL. */
data class OpenVexSubcomponentDto(
    @param:JsonProperty("@id")
    val id: String,
)
