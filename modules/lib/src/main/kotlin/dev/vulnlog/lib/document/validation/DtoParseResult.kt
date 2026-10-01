// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.dto.DtoVersion
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.yaml.dtoMapper
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.finding.ParseFailure
import org.snakeyaml.engine.v2.nodes.MappingNode
import tools.jackson.databind.DatabindException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.exc.UnrecognizedPropertyException

internal sealed interface DtoParseResult {
    data class Parsed(
        val dto: DtoVersion,
    ) : DtoParseResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : DtoParseResult
}

/** [rootNode] only locates a failure in the source. */
internal fun bindToDto(
    document: JsonNode,
    version: SchemaVersion,
    rootNode: MappingNode,
): DtoParseResult =
    when (version) {
        SchemaVersion.V1 -> bind(document, rootNode, VulnlogFileV1Dto::class.java)
    }

private fun bind(
    document: JsonNode,
    rootNode: MappingNode,
    type: Class<out DtoVersion>,
): DtoParseResult =
    try {
        DtoParseResult.Parsed(dtoMapper.treeToValue(document, type))
    } catch (e: UnrecognizedPropertyException) {
        rejected("Unknown property '${e.propertyName}'. Try updating vulnlog.", rootNode, e)
    } catch (e: DatabindException) {
        rejected("YAML parse error: ${e.originalMessage}", rootNode, e)
    }

private fun rejected(
    message: String,
    rootNode: MappingNode,
    e: DatabindException,
) = DtoParseResult.Rejected(listOf(failureAt(rootNode, e.path, message)))
