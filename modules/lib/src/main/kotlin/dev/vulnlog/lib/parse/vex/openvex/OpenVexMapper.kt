// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.OPEN_VEX_CONTEXT
import dev.vulnlog.lib.core.vex.openvex.OPEN_VEX_ROLE
import dev.vulnlog.lib.core.vex.openvex.openVexJustification
import dev.vulnlog.lib.core.vex.openvex.openVexStatus
import dev.vulnlog.lib.core.vex.openvex.openVexVulnerabilityUrl
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexDocumentDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexIdentifiersDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexProductDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexSubcomponentDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexVulnerabilityDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object OpenVexMapper {
    fun toDto(document: OpenVexDocument): OpenVexDocumentDto {
        val identity = document.identity
        return OpenVexDocumentDto(
            context = OPEN_VEX_CONTEXT,
            id = identity.id,
            author = document.author,
            role = OPEN_VEX_ROLE,
            // Formatted here rather than left to Jackson, whose date handling is version dependent.
            timestamp = format(identity.timestamp),
            version = identity.version,
            tooling = document.tooling,
            statements = document.statements.map { statement -> toStatementDto(statement, document) },
        )
    }

    /**
     * A YAML date is widened to midnight UTC. An entry that records none is dated by the document, written out rather
     * than inherited: the document timestamp moves with every version, and an inherited statement would move with it.
     */
    private fun toStatementDto(
        statement: OpenVexStatement,
        document: OpenVexDocument,
    ): OpenVexStatementDto {
        val timestamp = format(statement.timestamp?.let(::midnightUtc) ?: document.identity.timestamp)
        val actionStatement = actionStatementOf(statement.status)
        return OpenVexStatementDto(
            vulnerability = toVulnerabilityDto(statement.vulnerability),
            timestamp = timestamp,
            products = statement.products.map { purl -> toProductDto(purl, statement.subcomponents) },
            status = openVexStatus(statement.status),
            justification = justificationOf(statement.status),
            impactStatement = impactStatementOf(statement.status),
            actionStatement = actionStatement,
            actionStatementTimestamp = actionStatement?.let { timestamp },
            statusNotes = statusNotesOf(statement.status),
            supplier = document.supplier,
        )
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
                    .map { component ->
                        OpenVexSubcomponentDto(component.value)
                    }.takeIf { it.isNotEmpty() },
        )

    /** Required for `not_affected`, absent everywhere else. */
    private fun justificationOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.NotAffected -> openVexJustification(status.justification)
            is VexStatus.Affected, VexStatus.Fixed, is VexStatus.UnderInvestigation -> null
        }

    /** Required for `affected`, absent everywhere else. */
    private fun actionStatementOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.Affected -> status.actionStatement
            is VexStatus.NotAffected, VexStatus.Fixed, is VexStatus.UnderInvestigation -> null
        }

    /** The analysis behind a `not_affected` label, which the specification pairs with the justification. */
    private fun impactStatementOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.NotAffected -> status.impactStatement
            is VexStatus.Affected, VexStatus.Fixed, is VexStatus.UnderInvestigation -> null
        }

    /** How the status was determined. A `fixed` statement carries none: it would describe the state before the fix. */
    private fun statusNotesOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.Affected -> status.statusNotes
            is VexStatus.UnderInvestigation -> status.statusNotes
            is VexStatus.NotAffected, VexStatus.Fixed -> null
        }

    private fun format(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)

    private fun midnightUtc(date: LocalDate): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()
}
