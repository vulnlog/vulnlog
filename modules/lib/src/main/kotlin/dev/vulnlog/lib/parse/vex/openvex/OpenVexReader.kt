// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexBaselineDto
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Any OpenVEX namespace is accepted, so a v0.1.0 document can still be continued. */
private const val OPEN_VEX_NAMESPACE = "https://openvex.dev/ns"

/** Every revision carries its own, so they are removed before two documents are compared. */
private val VOLATILE_FIELDS = listOf("timestamp", "version")

/** A document without a `version` is the first revision. */
private const val FIRST_VERSION = 1

object OpenVexReader {
    /**
     * Reads the identity of an OpenVEX document from [content].
     *
     * Returns null when [content] is not one, so the caller starts a fresh identity rather than failing: garbage in,
     * new identity out. The timestamp must parse for the document to count, but it is not carried over, because every
     * revision is issued anew.
     */
    fun readBaseline(content: String): OpenVexBaseline? {
        val dto =
            try {
                openVexJson.readValue(content, OpenVexBaselineDto::class.java)
            } catch (_: JacksonException) {
                return null
            }
        if (dto.context?.startsWith(OPEN_VEX_NAMESPACE) != true) return null
        val id = dto.id?.takeIf(String::isNotBlank) ?: return null
        if (dto.timestamp?.let(::parseTimestamp) == null) return null
        return OpenVexBaseline(id = id, version = dto.version ?: FIRST_VERSION, content = content)
    }

    /**
     * True when [document] differs from [baseline] only in `version` and `timestamp`.
     *
     * Both sides are compared as trees, so key order and formatting do not matter. A baseline another tool wrote
     * carries fields this writer does not emit and therefore always compares as changed.
     */
    fun isUnchanged(
        baseline: OpenVexBaseline,
        document: OpenVexDocument,
    ): Boolean {
        val baselineTree =
            try {
                openVexJson.readTree(baseline.content)
            } catch (_: JacksonException) {
                return false
            }
        val documentTree = openVexJson.valueToTree<JsonNode>(OpenVexMapper.toDto(document))
        if (baselineTree !is ObjectNode || documentTree !is ObjectNode) return false
        // Both trees are freshly parsed and local to this call, so they can be stripped in place.
        baselineTree.remove(VOLATILE_FIELDS)
        documentTree.remove(VOLATILE_FIELDS)
        return baselineTree == documentTree
    }
}

/** Accepts any RFC 3339 offset, so a document written elsewhere still counts as one. */
private fun parseTimestamp(value: String): Instant? =
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
