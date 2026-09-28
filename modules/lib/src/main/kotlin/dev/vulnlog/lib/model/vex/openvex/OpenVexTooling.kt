// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** The Vulnlog that writes the document: the [platform] it runs on and its [version]. */
data class OpenVexTooling(
    val platform: String,
    val version: String,
) {
    init {
        require(platform.isNotBlank()) { "VEX tooling platform must not be blank" }
        require(version.isNotBlank()) { "VEX tooling version must not be blank" }
    }
}
