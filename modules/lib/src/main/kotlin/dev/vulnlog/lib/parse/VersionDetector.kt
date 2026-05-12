// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.SchemaVersion

fun parseSchemaVersion(raw: String): SchemaVersion? {
    val parts = raw.split(".")
    return SchemaVersion(
        major = parts[0].toIntOrNull() ?: return null,
        minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
    )
}
