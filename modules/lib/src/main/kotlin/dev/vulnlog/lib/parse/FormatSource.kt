// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFileRaw
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

/**
 * A parsed view of a Vulnlog file's presentation: the raw text plus the composed snakeyaml node
 * tree, which carries the styles and source positions that the DTO and domain models strip.
 */
data class FormatSource(
    val raw: VulnlogFileRaw,
    val root: MappingNode?,
) {
    companion object {
        fun of(raw: VulnlogFileRaw): FormatSource {
            val root = Compose(LoadSettings.builder().build()).composeString(raw.content).orElse(null)
            return FormatSource(raw, root as? MappingNode)
        }
    }
}

/** A value node and the path it sits at, e.g. `vulnerabilities[CVE-2026-0001].releases`. */
data class LocatedNode(
    val path: String,
    val node: Node,
)

/** 1-based line of [node] in the source text, or 0 when unknown. */
fun lineOf(node: Node): Int = node.startMark.map { it.line + 1 }.orElse(0)

/** The mapping's keys in document order. */
fun mappingKeys(mapping: MappingNode): List<String> = mapping.value.mapNotNull { (it.keyNode as? ScalarNode)?.value }

/** The scalar value under [key], or null when absent or not scalar. */
fun scalarValueOf(
    mapping: MappingNode,
    key: String,
): String? =
    mapping.value
        .firstOrNull { (it.keyNode as? ScalarNode)?.value == key }
        ?.let { (it.valueNode as? ScalarNode)?.value }

/**
 * Flattens every value node (mapping values and sequence items) with its path. Sequence items with
 * an `id` key are addressed by it (`vulnerabilities[CVE-X]`, `releases[1.0.0]`), others by index.
 */
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
