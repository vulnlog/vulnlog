// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

/** The presentation the DTO and domain model drop: the raw text and the node tree with styles and positions. */
data class FormatSource(
    val raw: String,
    val root: MappingNode,
)

/** [path] reads like `vulnerabilities[CVE-2026-0001].releases`. */
data class LocatedNode(
    val path: String,
    val node: Node,
)

/** 1-based, or 0 when unknown. */
fun lineOf(node: Node): Int = node.startMark.map { it.line + 1 }.orElse(0)

fun mappingKeys(mapping: MappingNode): List<String> = mapping.value.mapNotNull { (it.keyNode as? ScalarNode)?.value }

fun valueNodeOf(
    mapping: MappingNode,
    key: String,
): Node? =
    mapping.value
        .firstOrNull { (it.keyNode as? ScalarNode)?.value == key }
        ?.valueNode

fun scalarValueOf(
    mapping: MappingNode,
    key: String,
): String? = (valueNodeOf(mapping, key) as? ScalarNode)?.value

/** Items with an `id` are addressed by it (`releases[1.0.0]`), others by index. */
fun walkValues(root: MappingNode): List<LocatedNode> {
    val collected = mutableListOf<LocatedNode>()

    fun visit(
        path: String,
        node: Node,
    ) {
        collected.add(LocatedNode(path, node))
        when (node) {
            is MappingNode ->
                node.value.forEach { tuple ->
                    val key = (tuple.keyNode as? ScalarNode)?.value ?: return@forEach
                    visit("$path.$key", tuple.valueNode)
                }

            is SequenceNode ->
                node.value.forEachIndexed { index, item ->
                    val id = (item as? MappingNode)?.let { scalarValueOf(it, "id") }
                    visit("$path[${id ?: index}]", item)
                }

            else -> {}
        }
    }

    root.value.forEach { tuple ->
        val key = (tuple.keyNode as? ScalarNode)?.value ?: return@forEach
        visit(key, tuple.valueNode)
    }
    return collected
}
