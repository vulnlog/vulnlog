// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

// Who issues a VEX document and with what, the same in every format.

data class VexAuthor(
    val name: String,
    val contact: String? = null,
) {
    init {
        require(name.isNotBlank()) { "VEX author is required" }
        require(contact == null || contact.isNotBlank()) { "VEX author contact must not be blank" }
    }
}

@JvmInline
value class VexSupplier(
    val organization: String,
) {
    init {
        require(organization.isNotBlank()) { "VEX supplier must not be empty" }
    }
}

data class VexTooling(
    val platform: String,
    val version: String,
) {
    init {
        require(platform.isNotBlank()) { "VEX tooling platform must not be blank" }
        require(version.isNotBlank()) { "VEX tooling version must not be blank" }
    }
}
