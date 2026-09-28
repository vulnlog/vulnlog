// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.codec.openvex.dto.OpenVexDocumentDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexIdentifiersDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexProductDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexSubcomponentDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexVulnerabilityDto
import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object OpenVexEncoder {
    fun encode(document: OpenVexDocument): String = openVexJson.writeValueAsString(toDto(document)) + "\n"

    /** One branch per format version: a new version gets its own mapping rather than flags in this one. */
    internal fun toDto(document: OpenVexDocument): OpenVexDocumentDto =
        when (document.formatVersion) {
            OpenVexFormatVersion.VERSION_0_2_0 -> toDocumentDtoV020(document)
        }
}

private fun toDocumentDtoV020(document: OpenVexDocument): OpenVexDocumentDto =
    OpenVexDocumentDto(
        context = openVexContext(document.formatVersion),
        id = document.identity.id.value,
        author = openVexAuthor(document.author),
        role = OPEN_VEX_ROLE,
        timestamp = format(document.identity.timestamp),
        version = document.identity.version.value,
        tooling = document.tooling?.let(::openVexTooling),
        statements = document.statements.map { statement -> toStatementDto(statement, document) },
    )

/**
 * An issued statement is written with the document's time rather than left to inherit it: the document time moves
 * with every revision, and an inherited statement would silently move with it.
 */
private fun toStatementDto(
    statement: OpenVexStatement,
    document: OpenVexDocument,
): OpenVexStatementDto {
    val timestamp =
        when (val time = statement.timestamp) {
            is OpenVexStatementTime.Stated -> time.date.atStartOfDay(ZoneOffset.UTC).toInstant()
            is OpenVexStatementTime.Carried -> time.at
            OpenVexStatementTime.Issued -> document.identity.timestamp
        }.let(::format)
    val statementDto =
        OpenVexStatementDto(
            vulnerability = toVulnerabilityDto(statement.vulnerability),
            timestamp = timestamp,
            products = statement.products.map { purl -> toProductDto(purl, statement.subcomponents) },
            status = openVexStatus(vexStatusKind(statement.status)),
            supplier = document.supplier.organization,
        )
    return when (val status = statement.status) {
        is VexStatus.UnderInvestigation -> statementDto.copy(statusNotes = status.statusNotes)
        VexStatus.Fixed -> statementDto
        is VexStatus.NotAffected ->
            statementDto.copy(
                justification = openVexJustification(status.justification),
                impactStatement = status.impactStatement,
            )

        is VexStatus.Affected ->
            statementDto.copy(
                actionStatement = openVexActionStatement(status.remediation),
                actionStatementTimestamp = timestamp,
                statusNotes = status.statusNotes,
            )
    }
}

private fun toVulnerabilityDto(vulnerability: OpenVexVulnerability): OpenVexVulnerabilityDto =
    OpenVexVulnerabilityDto(
        id = openVexVulnerabilityUrl(vulnerability.id),
        name = vulnerability.id.id,
        description = vulnerability.description,
        aliases = vulnerability.aliases.map { alias -> alias.id }.takeIf { it.isNotEmpty() },
    )

private fun toProductDto(
    purl: Purl,
    subcomponents: List<Purl>,
): OpenVexProductDto =
    OpenVexProductDto(
        id = purl.value,
        identifiers = OpenVexIdentifiersDto(purl.value),
        subcomponents =
            subcomponents
                .map { component -> OpenVexSubcomponentDto(component.value, OpenVexIdentifiersDto(component.value)) }
                .takeIf { it.isNotEmpty() },
    )

/** Formatted here rather than by Jackson, whose date handling differs between versions. */
private fun format(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
