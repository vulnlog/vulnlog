// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.SchemaVersionParseResult
import dev.vulnlog.lib.document.parseSchemaVersion
import dev.vulnlog.lib.document.yaml.valueNodeOf
import dev.vulnlog.lib.finding.ParseFailure
import dev.vulnlog.lib.model.SchemaVersion
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode

sealed interface SchemaVersionResult {
    data class Recognized(
        val version: SchemaVersion,
    ) : SchemaVersionResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : SchemaVersionResult
}

private const val VERSION_KEY = "schemaVersion"

fun resolveSchemaVersion(rootNode: MappingNode): SchemaVersionResult {
    val versionNode: Node =
        valueNodeOf(rootNode, VERSION_KEY) ?: return rejected("Missing $VERSION_KEY")
    val rawVersion =
        (versionNode as? ScalarNode)?.value
            ?: return rejected("Invalid $VERSION_KEY", versionNode)

    return when (val parsed = parseSchemaVersion(rawVersion)) {
        is SchemaVersionParseResult.Malformed ->
            rejected("Invalid schema version '$rawVersion'", versionNode)

        is SchemaVersionParseResult.Unsupported ->
            rejected("Unsupported schema version '${parsed.raw}'. Try updating vulnlog.", versionNode)

        is SchemaVersionParseResult.Recognized -> SchemaVersionResult.Recognized(parsed.version)
    }
}

private fun rejected(
    message: String,
    node: Node? = null,
) = SchemaVersionResult.Rejected(listOf(ParseFailure(message, VERSION_KEY, node?.let(::locationOf))))
