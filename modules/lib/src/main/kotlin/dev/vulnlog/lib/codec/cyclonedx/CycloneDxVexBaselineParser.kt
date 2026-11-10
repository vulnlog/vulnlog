// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import com.github.packageurl.MalformedPackageURLException
import com.github.packageurl.PackageURL
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxBaselineDto
import dev.vulnlog.lib.codec.vex.remediationOf
import dev.vulnlog.lib.codec.vex.vexJson
import dev.vulnlog.lib.codec.vex.vexJustificationOf
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentityField
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

private const val VERSION_FIELD = "version"
private const val METADATA_FIELD = "metadata"
private const val TIMESTAMP_FIELD = "timestamp"

/**
 * The timestamp must parse, but a revision is issued anew and does not take it over. A baseline vulnerability this
 * writer cannot read is skipped rather than failing the baseline: it only loses its carried time.
 */
fun parseCycloneDxVexBaseline(
    content: String,
    requiredFormatVersion: CycloneDxVexFormatVersion,
): CycloneDxVexBaselineResult {
    val dto =
        try {
            vexJson.readValue(content, CycloneDxBaselineDto::class.java)
        } catch (_: JacksonException) {
            return rejected(CycloneDxVexBaselineProblem.NotCycloneDx)
        }
    if (dto.bomFormat != BOM_FORMAT) return rejected(CycloneDxVexBaselineProblem.NotCycloneDx)
    val declared =
        dto.specVersion?.takeIf(String::isNotBlank) ?: return rejected(CycloneDxVexBaselineProblem.NotCycloneDx)
    if (declared != requiredFormatVersion.version) {
        return rejected(CycloneDxVexBaselineProblem.OtherFormatVersion(declared, requiredFormatVersion))
    }
    return when (requiredFormatVersion) {
        CycloneDxVexFormatVersion.VERSION_1_7 -> baselineOf(dto, requiredFormatVersion)
    }
}

/**
 * Compared as trees, so key order and formatting do not count. A baseline written by another tool carries fields this
 * writer does not emit and therefore always counts as changed.
 */
fun sameCycloneDxVexContent(
    baselineContent: String,
    document: CycloneDxVexDocument,
): Boolean {
    val baselineTree =
        try {
            vexJson.readTree(baselineContent)
        } catch (_: JacksonException) {
            return false
        }
    val documentTree = vexJson.valueToTree<JsonNode>(CycloneDxVexEncoder.toDto(document))
    if (baselineTree !is ObjectNode || documentTree !is ObjectNode) return false
    return withoutVolatileFields(baselineTree) == withoutVolatileFields(documentTree)
}

/** Every revision writes the version and the timestamp anew, so they never count as a change. */
private fun withoutVolatileFields(tree: ObjectNode): ObjectNode {
    tree.remove(VERSION_FIELD)
    (tree.get(METADATA_FIELD) as? ObjectNode)?.remove(TIMESTAMP_FIELD)
    return tree
}

private fun baselineOf(
    dto: CycloneDxBaselineDto,
    formatVersion: CycloneDxVexFormatVersion,
): CycloneDxVexBaselineResult {
    val serialNumber =
        dto.serialNumber?.let(CycloneDxSerialNumber::parse)
            ?: return invalid(CycloneDxVexIdentityField.SERIAL_NUMBER, dto.serialNumber)
    val timestamp = dto.metadata?.get(TIMESTAMP_FIELD)?.textOrNull()
    val issuedAt = timestamp?.let(::parseTimestamp) ?: return invalid(CycloneDxVexIdentityField.TIMESTAMP, timestamp)
    val version =
        dto.version?.let { value ->
            VexDocumentVersion.parse(value) ?: return invalid(CycloneDxVexIdentityField.VERSION, value.toString())
        } ?: VexDocumentVersion.FIRST
    val packages = packagesOf(dto.components)
    val vulnerabilities =
        dto.vulnerabilities
            ?.takeIf(JsonNode::isArray)
            ?.values()
            .orEmpty()
            .mapNotNull { vulnerabilityOf(it, issuedAt, packages) }
    return CycloneDxVexBaselineResult.Parsed(
        CycloneDxVexBaseline(
            formatVersion = formatVersion,
            serialNumber = serialNumber,
            version = version,
            vulnerabilities = vulnerabilities,
        ),
    )
}

private fun rejected(problem: CycloneDxVexBaselineProblem): CycloneDxVexBaselineResult =
    CycloneDxVexBaselineResult.Rejected(problem)

private fun invalid(
    field: CycloneDxVexIdentityField,
    value: String?,
): CycloneDxVexBaselineResult = rejected(CycloneDxVexBaselineProblem.InvalidIdentity(field, value))

/** The packages by `bom-ref`, as `affects` names them; a component without a readable purl is left out. */
private fun packagesOf(components: JsonNode?): Map<String, Purl> =
    components
        ?.takeIf(JsonNode::isArray)
        ?.values()
        .orEmpty()
        .mapNotNull { component ->
            val ref = component.get("bom-ref")?.textOrNull() ?: return@mapNotNull null
            val purl = component.get("purl")?.textOrNull()?.let(::purlOf) ?: return@mapNotNull null
            ref to purl
        }.toMap()

/** An analysis without its own `firstIssued` carries the document's time, as an OpenVEX statement would. */
private fun vulnerabilityOf(
    node: JsonNode,
    issuedAt: Instant,
    packages: Map<String, Purl>,
): CycloneDxVexVulnerability? {
    val id = node.get("id")?.textOrNull()?.let(::vulnIdOf) ?: return null
    val aliases =
        node
            .get("references")
            ?.let { references -> references.takeIf(JsonNode::isArray)?.values() ?: return null }
            .orEmpty()
            .map { reference -> reference.get("id")?.textOrNull()?.let(::vulnIdOf) ?: return null }
    val description = node.get("description")?.let { description -> description.textOrNull() ?: return null }
    val affected =
        node
            .get("affects")
            ?.let { affects -> affects.takeIf(JsonNode::isArray)?.values() ?: return null }
            .orEmpty()
            .map { affect -> affect.get("ref")?.textOrNull()?.let(packages::get) ?: return null }
    val analysis = node.get("analysis")?.takeIf(JsonNode::isObject) ?: return null
    val firstIssued =
        analysis.get("firstIssued")?.let { time ->
            time.textOrNull()?.let(::parseTimestamp) ?: return null
        }
    val status = statusOf(node, analysis) ?: return null
    return CycloneDxVexVulnerability(
        id = id,
        aliases = aliases,
        description = description,
        packages = affected,
        status = status,
        firstIssued = VexStatementTime.Carried(firstIssued ?: issuedAt),
    )
}

private fun statusOf(
    node: JsonNode,
    analysis: JsonNode,
): VexStatus? {
    val detail = analysis.get("detail")?.textOrNull()
    return when (analysis.get("state")?.textOrNull()?.let(::cycloneDxStateKind) ?: return null) {
        VexStatusKind.UNDER_INVESTIGATION -> VexStatus.UnderInvestigation(detail)
        VexStatusKind.FIXED -> VexStatus.Fixed
        VexStatusKind.NOT_AFFECTED -> {
            val justification = justificationOf(node) ?: return null
            VexStatus.NotAffected(justification, detail)
        }

        VexStatusKind.AFFECTED -> {
            val remediation = node.get("recommendation")?.textOrNull()?.let(::remediationOf) ?: return null
            VexStatus.Affected(remediation, detail)
        }
    }
}

/** Read from the property this writer adds, because the CycloneDX justification alone is ambiguous. */
private fun justificationOf(node: JsonNode) =
    node
        .get("properties")
        ?.takeIf(JsonNode::isArray)
        ?.values()
        ?.firstOrNull { property -> property.get("name")?.textOrNull() == JUSTIFICATION_PROPERTY }
        ?.get("value")
        ?.textOrNull()
        ?.let(::vexJustificationOf)

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
