// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.OPEN_VEX_CONTEXT
import dev.vulnlog.lib.core.vex.openvex.openVexJustification
import dev.vulnlog.lib.core.vex.openvex.openVexStatus
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexDocumentDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexProductDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexVulnerabilityDto
import java.time.format.DateTimeFormatter

object OpenVexMapper {
    fun toDto(document: OpenVexDocument): OpenVexDocumentDto =
        OpenVexDocumentDto(
            context = OPEN_VEX_CONTEXT,
            id = document.id,
            author = document.author,
            // Formatted here rather than left to Jackson, whose date handling is version dependent.
            timestamp = DateTimeFormatter.ISO_INSTANT.format(document.timestamp),
            version = document.version,
            statements = document.statements.map(::toStatementDto),
        )

    private fun toStatementDto(statement: OpenVexStatement): OpenVexStatementDto =
        OpenVexStatementDto(
            vulnerability = OpenVexVulnerabilityDto(statement.vulnerability.id),
            products = statement.products.map { purl -> OpenVexProductDto(purl.value) },
            status = openVexStatus(statement.status),
            justification = justificationOf(statement.status),
            actionStatement = actionStatementOf(statement.status),
        )

    /** Required for `not_affected`, absent everywhere else. */
    private fun justificationOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.NotAffected -> openVexJustification(status.justification)
            is VexStatus.Affected, VexStatus.Fixed, VexStatus.UnderInvestigation -> null
        }

    /** Required for `affected`, absent everywhere else. */
    private fun actionStatementOf(status: VexStatus): String? =
        when (status) {
            is VexStatus.Affected -> status.actionStatement
            is VexStatus.NotAffected, VexStatus.Fixed, VexStatus.UnderInvestigation -> null
        }
}
