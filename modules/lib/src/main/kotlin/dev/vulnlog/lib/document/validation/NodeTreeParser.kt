// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.finding.FailureLocation
import dev.vulnlog.lib.finding.ParseFailure
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.exceptions.YamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.Tag

/** The node tree keeps styles and source positions, which later stages drop. */
fun parseToNodeTree(content: String): NodeTreeResult =
    try {
        val node: Node? = Compose(nodeTreeSettings()).composeString(content).orElse(null)
        when {
            isEmpty(node) -> rejected("Empty YAML document")
            node is MappingNode -> NodeTreeResult.Valid(node)
            else -> rejected("Invalid YAML format")
        }
    } catch (e: MarkedYamlEngineException) {
        rejected(e.problem ?: "Invalid YAML", locationOf(e))
    } catch (e: YamlEngineException) {
        rejected(e.message ?: "Invalid YAML")
    }

/** No text composes to no node, a bare `---` to a null scalar, and blank lines or comments alone to an empty mapping. */
private fun isEmpty(node: Node?): Boolean =
    node == null || (node is ScalarNode && node.tag == Tag.NULL) || (node is MappingNode && node.value.isEmpty())

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
