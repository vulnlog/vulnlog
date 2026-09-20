// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

private const val VULNLOG_SITE = "https://vulnlog.dev/"

@JvmInline
value class OpenVexTooling private constructor(
    val value: String,
) {
    constructor(platform: String, version: String) : this("Vulnlog $platform version $version, $VULNLOG_SITE") {
        require(platform.isNotBlank())
        require(version.isNotBlank())
    }
}
