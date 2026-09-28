// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

/** Every revision carries its own, so they are removed before two documents are compared. */
private val VOLATILE_FIELDS = listOf("timestamp", "version")

/**
 * True when [document] differs from the document in [baselineContent] only in the `version` and the `timestamp` every
 * revision writes anew. Statement times count: an untouched statement carries the baseline's, so only a change moves
 * one.
 *
 * Both sides are compared as trees, so key order and formatting do not matter. A baseline another tool wrote carries
 * fields this writer does not emit and therefore always compares as changed.
 */
fun sameOpenVexContent(
    baselineContent: String,
    document: OpenVexDocument,
): Boolean {
    val baselineTree =
        try {
            openVexJson.readTree(baselineContent)
        } catch (_: JacksonException) {
            return false
        }
    val documentTree = openVexJson.valueToTree<JsonNode>(OpenVexMapper.toDto(document))
    if (baselineTree !is ObjectNode || documentTree !is ObjectNode) return false
    // Both trees are freshly parsed and local to this call, so they can be stripped in place.
    stripVolatile(baselineTree)
    stripVolatile(documentTree)
    return baselineTree == documentTree
}

/** Removes from [tree] what every revision writes anew: the document `timestamp` and `version`. */
private fun stripVolatile(tree: ObjectNode) {
    tree.remove(VOLATILE_FIELDS)
}
