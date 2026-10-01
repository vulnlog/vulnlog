// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.yaml.dtoMapper
import dev.vulnlog.lib.model.finding.FailureLocation
import dev.vulnlog.lib.model.finding.ParseFailure
import org.snakeyaml.engine.v2.constructor.StandardConstructor
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import tools.jackson.databind.JsonNode
import java.util.Optional

internal sealed interface DocumentResult {
    data class Built(
        val document: JsonNode,
    ) : DocumentResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : DocumentResult
}

/** Applies anchors and tags and drops styles and positions: the value tree a schema check reads. */
internal fun constructDocument(rootNode: MappingNode): DocumentResult {
    val values =
        try {
            StandardConstructor(nodeTreeSettings()).constructSingleDocument(Optional.of(rootNode))
        } catch (e: MarkedYamlEngineException) {
            return rejected("YAML parse error: ${e.problem}", locationOf(e))
        } catch (e: YamlEngineException) {
            return rejected("YAML parse error: ${e.message}")
        }
    return DocumentResult.Built(dtoMapper.valueToTree(values))
}

private fun rejected(
    message: String,
    location: FailureLocation? = null,
) = DocumentResult.Rejected(listOf(ParseFailure(message, location = location)))
