// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** Who issues the document: the responsible team or person, and how to reach them when recorded. */
data class OpenVexAuthor(
    val name: String,
    val contact: String? = null,
) {
    init {
        require(name.isNotBlank()) { "VEX author is required" }
        require(contact == null || contact.isNotBlank()) { "VEX author contact must not be blank" }
    }
}
