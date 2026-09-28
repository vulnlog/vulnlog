// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.codec.openvex.dto.OpenVexDocumentDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexIdentifiersDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexProductDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexSubcomponentDto
import dev.vulnlog.lib.codec.openvex.dto.OpenVexVulnerabilityDto
import dev.vulnlog.lib.core.vex.openvex.OPEN_VEX_ROLE
import dev.vulnlog.lib.core.vex.openvex.openVexJustification
import dev.vulnlog.lib.core.vex.openvex.openVexStatus
import dev.vulnlog.lib.core.vex.openvex.openVexVulnerabilityUrl
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexAuthor
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Where the tooling line points readers to. */
private const val VULNLOG_SITE = "https://vulnlog.dev/"

object OpenVexMapper {
    /** Maps [document] to the shape its format version is written in. One branch, and one mapper, per version. */
    fun toDto(document: OpenVexDocument): OpenVexDocumentDto =
        when (document.formatVersion) {
            OpenVexFormatVersion.VERSION_0_2_0 -> toDocumentDtoV020(document)
        }

    private fun toDocumentDtoV020(document: OpenVexDocument): OpenVexDocumentDto {
        val identity = document.identity
        return OpenVexDocumentDto(
            context = openVexContext(document.formatVersion),
            id = identity.id.value,
            author = authorLine(document.author),
            role = OPEN_VEX_ROLE,
            // Formatted here rather than left to Jackson, whose date handling is version dependent.
            timestamp = format(identity.timestamp),
            version = identity.version.value,
            tooling = document.tooling?.let(::toolingLine),
            statements = document.statements.map { statement -> toStatementDto(statement, document) },
        )
    }

    /**
     * A YAML date is widened to midnight UTC, and a carried time is written as the baseline carries it. A statement
     * issued by this revision is dated by the document, written out rather than inherited: the document timestamp
     * moves with every version, and an inherited statement would move with it.
     */
    private fun toStatementDto(
        statement: OpenVexStatement,
        document: OpenVexDocument,
    ): OpenVexStatementDto {
        val timestamp =
            format(
                when (val time = statement.timestamp) {
                    is OpenVexStatementTime.Stated -> midnightUtc(time.date)
                    is OpenVexStatementTime.Carried -> time.at
                    OpenVexStatementTime.Issued -> document.identity.timestamp
                },
            )
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
            supplier = document.supplier.organization,
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
            is VexStatus.Affected -> actionStatementOf(status.remediation)
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

    /** The author, with the contact in parentheses when one is recorded. */
    private fun authorLine(author: OpenVexAuthor): String =
        author.contact?.let { contact -> "${author.name} ($contact)" } ?: author.name

    private fun toolingLine(tooling: OpenVexTooling): String =
        "Vulnlog ${tooling.platform} version ${tooling.version}, $VULNLOG_SITE"

    private fun format(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)

    private fun midnightUtc(date: LocalDate): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()
}
