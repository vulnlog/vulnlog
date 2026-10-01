// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import org.snakeyaml.engine.v2.comments.CommentLine
import org.snakeyaml.engine.v2.comments.CommentType
import org.snakeyaml.engine.v2.nodes.AnchorNode
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.SequenceNode

/**
 * Comments a canonical rewrite would drop; the `# $schema:` header survives and does not count. Needs a tree composed
 * with comment parsing, as the parse pipeline does.
 */
fun hasYamlComments(root: Node): Boolean =
    commentLines(root).any { comment ->
        comment.commentType != CommentType.BLANK_LINE && !isSchemaHeader(comment)
    }

/** The header only serves editors, so a write keeps it where it was and never adds it. */
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
