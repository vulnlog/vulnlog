// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.model.finding.ParseFailure
import dev.vulnlog.lib.parse.createYamlMapper
import org.snakeyaml.engine.v2.constructor.StandardConstructor
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import tools.jackson.databind.JsonNode
import java.util.Optional

/** Whether the node tree resolves into a value tree. */
sealed interface DocumentResult {
    data class Built(
        val document: JsonNode,
    ) : DocumentResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : DocumentResult
}

/**
 * Resolves the node tree into a value tree: anchors and tags are applied, styles and source
 * positions are dropped. This is the representation a schema check reads.
 */
fun constructDocument(rootNode: MappingNode): DocumentResult {
    val values =
        try {
            StandardConstructor(nodeTreeSettings()).constructSingleDocument(Optional.of(rootNode))
        } catch (e: MarkedYamlEngineException) {
            return rejected("YAML parse error: ${e.problem}", locationOf(e))
        } catch (e: YamlEngineException) {
            return rejected("YAML parse error: ${e.message}")
        }
    return DocumentResult.Built(createYamlMapper().valueToTree(values))
}

private fun rejected(
    message: String,
    location: dev.vulnlog.lib.model.finding.FailureLocation? = null,
) = DocumentResult.Rejected(listOf(ParseFailure(message, location = location)))
