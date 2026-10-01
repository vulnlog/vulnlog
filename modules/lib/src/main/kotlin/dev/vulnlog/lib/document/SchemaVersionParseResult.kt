// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.model.SchemaVersion

sealed interface SchemaVersionParseResult {
    data class Recognized(
        val version: SchemaVersion,
    ) : SchemaVersionParseResult

    data class Unsupported(
        val raw: String,
    ) : SchemaVersionParseResult

    data object Malformed : SchemaVersionParseResult
}

fun parseSchemaVersion(input: String): SchemaVersionParseResult {
    val parts = input.split(".")
    val major = parts[0].toIntOrNull() ?: return SchemaVersionParseResult.Malformed
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return if (major == 1 && minor == 0) {
        SchemaVersionParseResult.Recognized(SchemaVersion.V1)
    } else {
        SchemaVersionParseResult.Unsupported(input)
    }
}
