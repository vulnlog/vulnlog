// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.codec.openvex.dto.OpenVexBaselineDto
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** Every revision writes these anew, so they never count as a change. */
private val VOLATILE_FIELDS = listOf("timestamp", "version")

/**
 * The timestamp must parse, but a revision is issued anew and does not take it over. A baseline statement this writer
 * cannot read is skipped rather than failing the baseline: it only loses its carried time.
 */
fun parseOpenVexBaseline(
    content: String,
    requiredFormatVersion: OpenVexFormatVersion,
): OpenVexBaselineResult {
    val dto =
        try {
            openVexJson.readValue(content, OpenVexBaselineDto::class.java)
        } catch (_: JacksonException) {
            return rejected(OpenVexBaselineProblem.NotOpenVex)
        }
    val declared = dto.context?.let(::declaredOpenVexVersion) ?: return rejected(OpenVexBaselineProblem.NotOpenVex)
    if (declared != requiredFormatVersion.version) {
        return rejected(OpenVexBaselineProblem.OtherFormatVersion(declared, requiredFormatVersion))
    }
    return when (requiredFormatVersion) {
        OpenVexFormatVersion.VERSION_0_2_0 -> baselineOf(dto, requiredFormatVersion)
    }
}

/**
 * Compared as trees, so key order and formatting do not count. A baseline written by another tool carries fields this
 * writer does not emit and therefore always counts as changed.
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
    val documentTree = openVexJson.valueToTree<JsonNode>(OpenVexEncoder.toDto(document))
    if (baselineTree !is ObjectNode || documentTree !is ObjectNode) return false
    baselineTree.remove(VOLATILE_FIELDS)
    documentTree.remove(VOLATILE_FIELDS)
    return baselineTree == documentTree
}

private fun baselineOf(
    dto: OpenVexBaselineDto,
    formatVersion: OpenVexFormatVersion,
): OpenVexBaselineResult {
    val id = dto.id?.let(OpenVexDocumentId::parse) ?: return invalid(OpenVexIdentityField.ID, dto.id)
    val issuedAt =
        dto.timestamp?.let(::parseTimestamp) ?: return invalid(OpenVexIdentityField.TIMESTAMP, dto.timestamp)
    val version =
        dto.version?.let { value ->
            OpenVexDocumentVersion.parse(value) ?: return invalid(OpenVexIdentityField.VERSION, value.toString())
        } ?: OpenVexDocumentVersion.FIRST
    val statements =
        dto.statements
            ?.takeIf(
                JsonNode::isArray,
            )?.values()
            .orEmpty()
            .mapNotNull { statementOf(it, issuedAt) }
    return OpenVexBaselineResult.Parsed(OpenVexBaseline(formatVersion, id, version, statements))
}

private fun rejected(problem: OpenVexBaselineProblem): OpenVexBaselineResult = OpenVexBaselineResult.Rejected(problem)

private fun invalid(
    field: OpenVexIdentityField,
    value: String?,
): OpenVexBaselineResult = rejected(OpenVexBaselineProblem.InvalidIdentity(field, value))

/** A statement without its own timestamp inherits the document's, as the specification defines. */
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

private fun subcomponentsOf(product: JsonNode): List<Purl>? {
    val subcomponents = product.get("subcomponents") ?: return emptyList()
    if (!subcomponents.isArray) return null
    return subcomponents.values().map { component -> component.get("@id")?.textOrNull()?.let(::purlOf) ?: return null }
}

private fun statusOf(node: JsonNode): VexStatus? {
    val notes = node.get("status_notes")?.textOrNull()
    return when (node.get("status")?.textOrNull()?.let(::openVexStatusKind) ?: return null) {
        VexStatusKind.UNDER_INVESTIGATION -> VexStatus.UnderInvestigation(notes)
        VexStatusKind.FIXED -> VexStatus.Fixed
        VexStatusKind.NOT_AFFECTED -> {
            val justification = node.get("justification")?.textOrNull()?.let(::openVexJustificationOf) ?: return null
            VexStatus.NotAffected(justification, node.get("impact_statement")?.textOrNull())
        }

        VexStatusKind.AFFECTED -> {
            val remediation = node.get("action_statement")?.textOrNull()?.let(::openVexRemediation) ?: return null
            VexStatus.Affected(remediation, notes)
        }
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

/** Any RFC 3339 offset, so a document written by another tool still counts. */
private fun parseTimestamp(value: String): Instant? =
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
