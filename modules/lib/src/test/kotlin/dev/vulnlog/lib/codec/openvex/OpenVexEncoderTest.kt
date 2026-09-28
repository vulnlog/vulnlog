// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.codec.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

// The golden test pins every field a Vulnlog file can produce. These cases need a statement the file cannot state.

private val identity =
    OpenVexIdentity(
        OpenVexDocumentId("https://vulnlog.dev/vex/abc"),
        Instant.parse("2026-04-25T00:00:00Z"),
        OpenVexDocumentVersion.FIRST,
    )
private val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")
private val affected = VexStatus.Affected(Remediation.UpdateTo(release("1.0.1")))

private fun statement(
    status: VexStatus,
    timestamp: OpenVexStatementTime = OpenVexStatementTime.Issued,
): OpenVexStatement =
    OpenVexStatement(
        vulnerability = OpenVexVulnerability(cve("CVE-2026-1111"), aliases = emptyList(), description = null),
        timestamp = timestamp,
        products = listOf(Purl.Maven("pkg:maven/com.acme/app@1.0.0")),
        subcomponents = emptyList(),
        status = status,
    )

private fun dtoOf(statement: OpenVexStatement): OpenVexStatementDto =
    OpenVexEncoder.toDto(buildOpenVexDocument(project, identity, listOf(statement))).statements.single()

class OpenVexEncoderTest :
    FunSpec({

        test("under_investigation carries the analysis as status notes") {
            val statement = statement(VexStatus.UnderInvestigation("waiting on the upstream advisory"))

            val dto = dtoOf(statement)

            dto.statusNotes shouldBe "waiting on the upstream advisory"
        }

        test("a carried time is written as the baseline carries it") {
            val carried = OpenVexStatementTime.Carried(Instant.parse("2026-04-20T08:30:00Z"))

            val dto = dtoOf(statement(affected, carried))

            dto.timestamp shouldBe "2026-04-20T08:30:00Z"
            dto.actionStatementTimestamp shouldBe "2026-04-20T08:30:00Z"
        }

        test("an issued statement is written with the document's time, so it never inherits a later one") {
            val dto = dtoOf(statement(affected, OpenVexStatementTime.Issued))

            dto.timestamp shouldBe "2026-04-25T00:00:00Z"
            dto.actionStatementTimestamp shouldBe "2026-04-25T00:00:00Z"
        }
    })
