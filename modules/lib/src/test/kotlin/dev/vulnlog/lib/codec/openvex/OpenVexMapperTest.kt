// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.codec.openvex.dto.OpenVexStatementDto
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.freshOpenVexIdentity
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val identity = freshOpenVexIdentity(OpenVexDocumentId("https://vulnlog.dev/vex/abc"), ISSUED_AT)
private val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")
private val products = listOf(Purl.Maven("pkg:maven/com.acme/app@1.0.0"))
private val UPDATE_TO_1_0_1 = Remediation.UpdateTo(Release("1.0.1"))

private fun statement(
    status: VexStatus,
    timestamp: OpenVexStatementTime = OpenVexStatementTime.Issued,
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
                val affected = VexStatus.Affected(UPDATE_TO_1_0_1, "the parser is reachable")

                val dto = dtoOf(statement(affected, timestamp = OpenVexStatementTime.Stated(LocalDate.of(2026, 4, 7))))

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
                val dto =
                    dtoOf(statement(VexStatus.Fixed, timestamp = OpenVexStatementTime.Stated(LocalDate.of(2026, 4, 6))))

                dto.timestamp shouldBe "2026-04-06T00:00:00Z"
            }

            test("a carried time is written as the baseline carries it") {
                val carried = OpenVexStatementTime.Carried(Instant.parse("2026-04-20T08:30:00Z"))

                val dto = dtoOf(statement(VexStatus.Affected(UPDATE_TO_1_0_1), timestamp = carried))

                dto.timestamp shouldBe "2026-04-20T08:30:00Z"
                dto.actionStatementTimestamp shouldBe "2026-04-20T08:30:00Z"
            }

            test("a statement this revision issues is written with the document's time, never left to inherit") {
                val dto = dtoOf(statement(VexStatus.Affected(UPDATE_TO_1_0_1)))

                dto.timestamp shouldBe "2026-04-25T00:00:00Z"
                dto.actionStatementTimestamp shouldBe "2026-04-25T00:00:00Z"
            }
        }

        context("document") {

            test("names the role, the supplier and the tooling") {
                val tooling = OpenVexTooling("CLI", "0.18.0")
                val document = buildOpenVexDocument(project, identity, listOf(statement(VexStatus.Fixed)), tooling)

                val dto = OpenVexMapper.toDto(document)

                dto.role shouldBe "Document Creator"
                dto.tooling shouldBe "Vulnlog CLI version 0.18.0, https://vulnlog.dev/"
                dto.statements.single().supplier shouldBe "Acme Corp"
            }

            test("names the author, with the contact in parentheses when one is recorded") {
                val withContact = project.copy(contact = "security@acme.example")

                OpenVexMapper.toDto(buildOpenVexDocument(project, identity, emptyList())).author shouldBe
                    "Acme Security Team"
                OpenVexMapper.toDto(buildOpenVexDocument(withContact, identity, emptyList())).author shouldBe
                    "Acme Security Team (security@acme.example)"
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
