// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.model.finding.FailureLocation
import dev.vulnlog.lib.model.finding.ParseFailure
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node

/** Composes the input text into a node tree, which carries the styles and source positions. */
fun parseToNodeTree(content: String): NodeTreeResult =
    try {
        val node: Node =
            Compose(nodeTreeSettings()).composeString(content).orElse(null)
                ?: return rejected("Invalid YAML format")
        val rootNode: MappingNode = node as? MappingNode ?: return rejected("Invalid YAML format")
        NodeTreeResult.Valid(rootNode)
    } catch (e: MarkedYamlEngineException) {
        rejected(e.problem ?: "Invalid YAML", locationOf(e))
    } catch (e: YamlEngineException) {
        rejected(e.message ?: "Invalid YAML")
    }

/** Keeps marks and comments, and accepts duplicate keys so a rule can report them itself. */
internal fun nodeTreeSettings(): LoadSettings =
    LoadSettings
        .builder()
        .setUseMarks(true)
        .setAllowDuplicateKeys(true)
        .setParseComments(true)
        .build()

private fun rejected(
    message: String,
    location: FailureLocation? = null,
) = NodeTreeResult.Rejected(listOf(ParseFailure(message, location = location)))
