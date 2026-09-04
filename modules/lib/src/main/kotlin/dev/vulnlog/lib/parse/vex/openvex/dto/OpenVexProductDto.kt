// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex.dto

import com.fasterxml.jackson.annotation.JsonProperty

/** A product a statement applies to, identified by its Package URL. */
data class OpenVexProductDto(
    @param:JsonProperty("@id")
    val id: String,
)
