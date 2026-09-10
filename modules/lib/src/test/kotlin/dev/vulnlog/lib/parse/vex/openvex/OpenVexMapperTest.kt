// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.freshOpenVexIdentity
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import dev.vulnlog.lib.parse.vex.openvex.dto.OpenVexStatementDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val identity = freshOpenVexIdentity("https://vulnlog.dev/vex/abc", ISSUED_AT)
private val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")
private val products = listOf(Purl.Maven("pkg:maven/com.acme/app@1.0.0"))

private fun statement(
    status: VexStatus,
    timestamp: LocalDate? = null,
): OpenVexStatement =
    OpenVexStatement(
        vulnerability = OpenVexVulnerability(cve("CVE-2026-1111"), aliases = emptyList(), description = null),
        timestamp = timestamp,
        products = products,
        subcomponents = emptyList(),
        status = status,
    )

private fun dtoOf(statement: OpenVexStatement): OpenVexStatementDto =
    OpenVexMapper.toDto(buildOpenVexDocument(project, identity, listOf(statement))).statements.single()

class OpenVexMapperTest :
    FunSpec({

        context("analysis routing") {

            test("not_affected pairs the justification label with the analysis as impact statement") {
                val notAffected =
                    VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT, "the component is not shipped")

                val dto = dtoOf(statement(notAffected))

                dto.justification shouldBe "component_not_present"
                dto.impactStatement shouldBe "the component is not shipped"
                dto.statusNotes.shouldBeNull()
                dto.actionStatement.shouldBeNull()
            }

            test("affected carries the analysis as status notes and dates the action") {
                val affected = VexStatus.Affected("Update to release 1.0.1.", "the parser is reachable")

                val dto = dtoOf(statement(affected, timestamp = LocalDate.of(2026, 4, 7)))

                dto.statusNotes shouldBe "the parser is reachable"
                dto.impactStatement.shouldBeNull()
                dto.actionStatement shouldBe "Update to release 1.0.1."
                dto.actionStatementTimestamp shouldBe "2026-04-07T00:00:00Z"
            }

            test("under_investigation carries the analysis as status notes") {
                val dto = dtoOf(statement(VexStatus.UnderInvestigation("waiting on the upstream advisory")))

                dto.statusNotes shouldBe "waiting on the upstream advisory"
                dto.justification.shouldBeNull()
                dto.impactStatement.shouldBeNull()
                dto.actionStatement.shouldBeNull()
            }

            test("fixed carries no analysis text, because it would describe the state before the fix") {
                val dto = dtoOf(statement(VexStatus.Fixed))

                dto.statusNotes.shouldBeNull()
                dto.justification.shouldBeNull()
                dto.impactStatement.shouldBeNull()
                dto.actionStatement.shouldBeNull()
                dto.actionStatementTimestamp.shouldBeNull()
            }
        }

        context("timestamps") {

            test("a YAML date is widened to midnight UTC") {
                val dto = dtoOf(statement(VexStatus.Fixed, timestamp = LocalDate.of(2026, 4, 6)))

                dto.timestamp shouldBe "2026-04-06T00:00:00Z"
            }

            test("a statement without a date is written with the document's, never left to inherit") {
                val dto = dtoOf(statement(VexStatus.Affected("Update to release 1.0.1.")))

                dto.timestamp shouldBe "2026-04-25T00:00:00Z"
                dto.actionStatementTimestamp shouldBe "2026-04-25T00:00:00Z"
            }
        }

        context("document") {

            test("names the role, the supplier and the tooling") {
                val tooling = "Vulnlog CLI version 0.18.0, https://vulnlog.dev/"
                val document = buildOpenVexDocument(project, identity, listOf(statement(VexStatus.Fixed)), tooling)

                val dto = OpenVexMapper.toDto(document)

                dto.role shouldBe "Document Creator"
                dto.tooling shouldBe tooling
                dto.statements.single().supplier shouldBe "Acme Corp"
            }

            test("empty aliases and subcomponents are left out") {
                val dto = dtoOf(statement(VexStatus.Fixed))

                dto.vulnerability.aliases.shouldBeNull()
                dto.products
                    .single()
                    .subcomponents
                    .shouldBeNull()
                dto.products
                    .single()
                    .identifiers.purl shouldBe "pkg:maven/com.acme/app@1.0.0"
            }
        }
    })
