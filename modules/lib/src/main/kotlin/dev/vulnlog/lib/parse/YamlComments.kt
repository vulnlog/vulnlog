// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import org.snakeyaml.engine.v2.comments.CommentLine
import org.snakeyaml.engine.v2.comments.CommentType
import org.snakeyaml.engine.v2.nodes.AnchorNode
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.SequenceNode

/**
 * True when [root] carries YAML comments that a canonical rewrite drops. Blank lines and the
 * `# $schema:` header (which [YamlWriter] preserves) do not count as comments. Comments are only
 * present when the tree was composed with comment parsing enabled, as the parse pipeline does.
 */
fun hasYamlComments(root: Node): Boolean =
    commentLines(root).any { comment ->
        comment.commentType != CommentType.BLANK_LINE && !isSchemaHeader(comment)
    }

/**
 * True when [root] carries the optional `# $schema:` header comment. The header only enables JSON
 * Schema support in editors; [YamlWriter] re-emits it for documents that already have it and omits
 * it for those that do not, so a write never introduces it on its own.
 */
fun hasSchemaHeader(root: Node): Boolean = commentLines(root).any(::isSchemaHeader)

private fun isSchemaHeader(comment: CommentLine): Boolean = comment.value.trim().startsWith("\$schema:")

private fun commentLines(node: Node): List<CommentLine> {
    val comments = mutableListOf<CommentLine>()

    fun visit(node: Node) {
        node.blockComments?.let(comments::addAll)
        node.inLineComments?.let(comments::addAll)
        node.endComments?.let(comments::addAll)
        when (node) {
            is MappingNode ->
                node.value.forEach { tuple ->
                    visit(tuple.keyNode)
                    visit(tuple.valueNode)
                }

            is SequenceNode -> node.value.forEach(::visit)
            is AnchorNode -> visit(node.realNode)
            else -> {}
        }
    }

    visit(node)
    return comments
}
