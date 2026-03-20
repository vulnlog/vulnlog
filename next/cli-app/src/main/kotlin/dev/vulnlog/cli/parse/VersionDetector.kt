package dev.vulnlog.cli.parse

import dev.vulnlog.cli.model.SchemaVersion

fun parseSchemaVersion(raw: String): SchemaVersion? {
    val parts = raw.split(".")
    return SchemaVersion(
        major = parts[0].toIntOrNull() ?: return null,
        minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
    )
}
