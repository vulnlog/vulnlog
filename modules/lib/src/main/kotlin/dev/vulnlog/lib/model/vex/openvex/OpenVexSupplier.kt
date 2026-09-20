// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

/** The supplier of the VEX document */
@JvmInline
value class OpenVexSupplier(
    val organization: String,
) {
    init {
        require(organization.isNotBlank()) { "VEX supplier must not be empty" }
    }
}
