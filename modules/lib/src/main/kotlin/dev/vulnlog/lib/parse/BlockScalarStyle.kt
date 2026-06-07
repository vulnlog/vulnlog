// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFileRaw
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

/** The four YAML block-scalar flavours, split by indicator (`>`/`|`) and chomping (clip keeps a trailing newline). */
enum class BlockScalarStyle(
    val scalarStyle: ScalarStyle,
    val clip: Boolean,
) {
    FOLDED(ScalarStyle.FOLDED, clip = true), // >
    FOLDED_STRIP(ScalarStyle.FOLDED, clip = false), // >-
    LITERAL(ScalarStyle.LITERAL, clip = true), // |
    LITERAL_STRIP(ScalarStyle.LITERAL, clip = false), // |-
    ;

    companion object {
        fun fromNode(node: ScalarNode): BlockScalarStyle? =
            when (node.scalarStyle) {
                ScalarStyle.FOLDED -> if (node.value.endsWith("\n")) FOLDED else FOLDED_STRIP
                ScalarStyle.LITERAL -> if (node.value.endsWith("\n")) LITERAL else LITERAL_STRIP
                else -> null
            }
    }
}

/**
 * Maps each block scalar's value to the style it was written with, by composing [raw] with snakeyaml-engine
 * (which keeps scalar styles, unlike the Jackson DTO path). The value is keyed whitespace-trimmed to match the
 * representer (which trims values before rendering) and because both parsers fold scalars to the same text.
 *
 * If the same text appears both as a block scalar and a plain scalar, the block style wins for both — rare and
 * acceptable. Plain and quoted scalars are ignored.
 */
fun detectBlockScalarStyles(raw: VulnlogFileRaw): Map<String, BlockScalarStyle> {
    if (raw.content.isBlank()) return emptyMap()
    val root = Compose(LoadSettings.builder().build()).composeString(raw.content).orElse(null) ?: return emptyMap()

    val styles = mutableMapOf<String, BlockScalarStyle>()

    fun walk(node: Node) {
        when (node) {
            is MappingNode ->
                node.value.forEach {
                    walk(it.keyNode)
                    walk(it.valueNode)
                }
            is SequenceNode -> node.value.forEach(::walk)
            is ScalarNode -> BlockScalarStyle.fromNode(node)?.let { styles[node.value.trim()] = it }
        }
    }
    walk(root)
    return styles
}
