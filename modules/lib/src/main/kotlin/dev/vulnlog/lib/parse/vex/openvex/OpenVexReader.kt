// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.core.vex.openvex.openVexJustification
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexBaselineDto
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Every revision carries its own, so they are removed before two documents are compared. */
private val VOLATILE_FIELDS = listOf("timestamp", "version")

object OpenVexReader {
    /**
     * Reads the identity of an OpenVEX document from [content], required to be in [requiredFormatVersion].
     *
     * Every outcome but [OpenVexBaselineOutcome.Read] is the caller's to reject. A file that is no OpenVEX document
     * at all reads as [OpenVexBaselineOutcome.NotADocument]. A document in another format version reads as
     * [OpenVexBaselineOutcome.OtherFormatVersion], because continuing it would write one version's identity into
     * another version's bytes. A document whose identity cannot be continued reads as
     * [OpenVexBaselineOutcome.InvalidIdentity]. The timestamp must parse, but it is not carried over: every revision
     * is issued anew. The statements this writer can read back come with the baseline, so an untouched statement
     * keeps its time.
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
            dto.context?.let(::declaredOpenVexVersion)
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
     * True when [document] differs from [baseline] only in the `version` and the `timestamp` every revision writes
     * anew. Statement times count: an untouched statement carries the baseline's, so only a change moves one.
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

/**
 * The baseline [dto] describes, or [OpenVexBaselineOutcome.InvalidIdentity] naming the first identity field that
 * cannot be continued: an `@id` that is no absolute IRI, a missing or unparsable `timestamp`, or a `version` without a
 * successor.
 */
private fun baselineOf(
    dto: OpenVexBaselineDto,
    content: String,
    formatVersion: OpenVexFormatVersion,
): OpenVexBaselineOutcome {
    val id =
        dto.id?.let(OpenVexDocumentId::parse)
            ?: return OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.ID, dto.id)
    val issuedAt =
        dto.timestamp?.let(::parseTimestamp)
            ?: return OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.TIMESTAMP, dto.timestamp)
    val version =
        dto.version?.let { value ->
            OpenVexDocumentVersion.parse(value)
                ?: return OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.VERSION, value.toString())
        } ?: OpenVexDocumentVersion.FIRST
    return OpenVexBaselineOutcome.Read(
        OpenVexBaseline(
            formatVersion = formatVersion,
            id = id,
            version = version,
            content = content,
            statements = statementsOf(dto.statements, issuedAt),
        ),
    )
}

/**
 * The statements of [node] this writer can read back, each carrying its own time, or [issuedAt] when it inherits the
 * document's. A statement this writer would not write is skipped: nothing is matched against it.
 */
private fun statementsOf(
    node: JsonNode?,
    issuedAt: Instant,
): List<OpenVexStatement> =
    node
        ?.takeIf(JsonNode::isArray)
        ?.values()
        .orEmpty()
        .mapNotNull { statement -> statementOf(statement, issuedAt) }

private fun statementOf(
    node: JsonNode,
    issuedAt: Instant,
): OpenVexStatement? {
    val at = node.get("timestamp")?.let { timestamp -> timestamp.textOrNull()?.let(::parseTimestamp) ?: return null }
    val vulnerability = node.get("vulnerability")?.let(::vulnerabilityOf) ?: return null
    val products =
        node
            .get("products")
            ?.takeIf(JsonNode::isArray)
            ?.values()
            ?.takeIf { it.isNotEmpty() } ?: return null
    val purls = products.map { product -> product.get("@id")?.textOrNull()?.let(::purlOf) ?: return null }
    // This writer lists the same subcomponents on every product of a statement.
    val subcomponents =
        products.map { product -> subcomponentsOf(product) ?: return null }.distinct().singleOrNull() ?: return null
    val status = statusOf(node) ?: return null
    return OpenVexStatement(vulnerability, OpenVexStatementTime.Carried(at ?: issuedAt), purls, subcomponents, status)
}

private fun vulnerabilityOf(node: JsonNode): OpenVexVulnerability? {
    val id = node.get("name")?.textOrNull()?.let(::vulnIdOf) ?: return null
    val aliases = node.get("aliases")?.let { aliases -> textsOf(aliases) ?: return null }.orEmpty()
    val description = node.get("description")?.let { description -> description.textOrNull() ?: return null }
    return OpenVexVulnerability(id, aliases.map { alias -> vulnIdOf(alias) ?: return null }, description)
}

/** The subcomponents of [product], none when it lists none, or null when one of them cannot be read. */
private fun subcomponentsOf(product: JsonNode): List<Purl>? {
    val subcomponents = product.get("subcomponents") ?: return emptyList()
    if (!subcomponents.isArray) return null
    return subcomponents.values().map { component -> component.get("@id")?.textOrNull()?.let(::purlOf) ?: return null }
}

/** The status a statement states, with the text the status carries. The reverse of [OpenVexMapper]'s tokens. */
private fun statusOf(node: JsonNode): VexStatus? {
    val notes = node.get("status_notes")?.textOrNull()
    return when (node.get("status")?.textOrNull()) {
        "under_investigation" -> VexStatus.UnderInvestigation(notes)
        "fixed" -> VexStatus.Fixed
        "not_affected" -> {
            val token = node.get("justification")?.textOrNull()
            val justification =
                VexJustification.entries.firstOrNull { openVexJustification(it) == token } ?: return null
            VexStatus.NotAffected(justification, node.get("impact_statement")?.textOrNull())
        }

        "affected" ->
            VexStatus.Affected(node.get("action_statement")?.textOrNull()?.let(::remediationOf) ?: return null, notes)
        else -> null
    }
}

private fun textsOf(node: JsonNode): List<String>? =
    node.takeIf(JsonNode::isArray)?.values()?.map { value -> value.textOrNull() ?: return null }

private fun JsonNode.textOrNull(): String? = takeIf(JsonNode::isString)?.stringValue()

private fun vulnIdOf(value: String): VulnId? =
    try {
        parseVulnId(value)
    } catch (_: IllegalArgumentException) {
        null
    }

private fun purlOf(value: String): Purl? =
    try {
        parsePurl(PackageURL(value))
    } catch (_: MalformedPackageURLException) {
        null
    }

/** Removes from [tree] what every revision writes anew: the document `timestamp` and `version`. */
private fun stripVolatile(tree: ObjectNode) {
    tree.remove(VOLATILE_FIELDS)
}

/** Accepts any RFC 3339 offset, so a document written elsewhere still counts as one. */
private fun parseTimestamp(value: String): Instant? =
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
