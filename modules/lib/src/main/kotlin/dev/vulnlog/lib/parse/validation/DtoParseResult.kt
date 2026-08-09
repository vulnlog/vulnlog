// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.finding.ParseFailure
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.dto.DtoVersion
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto
import org.snakeyaml.engine.v2.nodes.MappingNode
import tools.jackson.databind.DatabindException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.exc.UnrecognizedPropertyException

/** Whether the value tree binds to the DTO of the declared schema version. */
sealed interface DtoParseResult {
    data class Parsed(
        val dto: DtoVersion,
    ) : DtoParseResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : DtoParseResult
}

/** Binds the value tree to the DTO. [rootNode] only serves to resolve where a failure sits. */
fun bindToDto(
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
        DtoParseResult.Parsed(createYamlMapper().treeToValue(document, type))
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
