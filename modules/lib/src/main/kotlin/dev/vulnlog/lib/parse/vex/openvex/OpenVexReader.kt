// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexBaselineDto
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Every revision carries its own, so they are removed before two documents are compared. */
private val VOLATILE_FIELDS = listOf("timestamp", "version")

/** A statement the file leaves undated is dated by the document, so these move with every revision as well. */
private val INHERITED_STATEMENT_FIELDS = listOf("timestamp", "action_statement_timestamp")

/** A document without a `version` is the first revision. */
private const val FIRST_VERSION = 1

object OpenVexReader {
    /**
     * Reads the identity of an OpenVEX document from [content], required to be in [requiredFormatVersion].
     *
     * A file that is no OpenVEX document at all reads as [OpenVexBaselineOutcome.NotADocument], so the caller starts
     * a fresh identity rather than failing: garbage in, new identity out. A document in another format version reads
     * as [OpenVexBaselineOutcome.OtherFormatVersion] and is the caller's to reject, because continuing it would
     * write one version's identity into another version's bytes. The timestamp must parse for the file to count as
     * a document, but it is not carried over: every revision is issued anew.
     */
    fun readBaseline(
        content: String,
        requiredFormatVersion: OpenVexFormatVersion,
    ): OpenVexBaselineOutcome {
        val dto =
            try {
                openVexJson.readValue(content, OpenVexBaselineDto::class.java)
            } catch (_: JacksonException) {
                return OpenVexBaselineOutcome.NotADocument
            }
        val declared =
            dto.context?.let(OpenVexFormatVersion::declaredVersion)
                ?: return OpenVexBaselineOutcome.NotADocument
        if (declared != requiredFormatVersion.version) {
            return OpenVexBaselineOutcome.OtherFormatVersion(declared, requiredFormatVersion)
        }
        // The identity fields are read as the required version places them. A version that moves one binds here.
        return when (requiredFormatVersion) {
            OpenVexFormatVersion.VERSION_0_2_0 -> baselineOf(dto, content, requiredFormatVersion)
        }
    }

    /**
     * True when [document] differs from [baseline] only in `version` and the timestamps every revision writes anew.
     *
     * Both sides are compared as trees, so key order and formatting do not matter. A baseline another tool wrote
     * carries fields this writer does not emit and therefore always compares as changed.
     */
    fun isUnchanged(
        baseline: OpenVexBaseline,
        document: OpenVexDocument,
    ): Boolean {
        require(baseline.formatVersion == document.formatVersion) {
            "cannot compare an OpenVEX ${baseline.formatVersion.version} baseline " +
                "with an OpenVEX ${document.formatVersion.version} document"
        }
        val baselineTree =
            try {
                openVexJson.readTree(baseline.content)
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
}

/** The baseline [dto] describes, or [OpenVexBaselineOutcome.NotADocument] when it carries no identity to continue. */
private fun baselineOf(
    dto: OpenVexBaselineDto,
    content: String,
    formatVersion: OpenVexFormatVersion,
): OpenVexBaselineOutcome {
    val id = dto.id?.takeIf(String::isNotBlank) ?: return OpenVexBaselineOutcome.NotADocument
    if (dto.timestamp?.let(::parseTimestamp) == null) return OpenVexBaselineOutcome.NotADocument
    return OpenVexBaselineOutcome.Read(
        OpenVexBaseline(
            formatVersion = formatVersion,
            id = id,
            version = dto.version ?: FIRST_VERSION,
            content = content,
        ),
    )
}

/**
 * Removes from [tree] what a revision writes anew: the document `timestamp` and `version`, and the statement dates
 * that were inherited from that timestamp rather than stated by the file. A date the file states is left in place, so
 * moving it still counts as a change.
 */
private fun stripVolatile(tree: ObjectNode) {
    val issuedAt = tree.get("timestamp")?.asString()
    tree.remove(VOLATILE_FIELDS)
    // Nothing was inherited when the document carries no timestamp of its own.
    if (issuedAt == null) return
    val statements = tree.get("statements") as? ArrayNode ?: return
    statements.filterIsInstance<ObjectNode>().forEach { statement ->
        INHERITED_STATEMENT_FIELDS
            .filter { field -> statement.get(field)?.asString() == issuedAt }
            .forEach { field -> statement.remove(field) }
    }
}

/** Accepts any RFC 3339 offset, so a document written elsewhere still counts as one. */
private fun parseTimestamp(value: String): Instant? =
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
