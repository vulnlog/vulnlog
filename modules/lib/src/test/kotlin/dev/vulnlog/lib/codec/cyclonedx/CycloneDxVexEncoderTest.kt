// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import com.networknt.schema.InputFormat
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxBomDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxPropertyDto
import dev.vulnlog.lib.codec.cyclonedx.dto.CycloneDxVulnerabilityDto
import dev.vulnlog.lib.core.vex.cyclonedx.buildCycloneDxVexDocument
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

// The golden test pins every field a Vulnlog file can produce. These cases need a vulnerability the file cannot state.

private val identity =
    CycloneDxVexIdentity(
        CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79"),
        Instant.parse("2026-04-25T00:00:00Z"),
        VexDocumentVersion.FIRST,
    )
private val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")
private val product = Purl.Maven("pkg:maven/com.acme/app@1.0.0")
private val lib = Purl.Npm("pkg:npm/example-lib@2.3.0")
private val parser = Purl.Npm("pkg:npm/example-parser@1.0.0")

private fun vulnerability(
    status: VexStatus,
    firstIssued: VexStatementTime = VexStatementTime.Issued,
    id: String = "CVE-2026-1111",
    packages: List<Purl> = listOf(lib),
): CycloneDxVexVulnerability =
    CycloneDxVexVulnerability(
        id = cve(id),
        aliases = emptyList(),
        description = null,
        packages = packages,
        status = status,
        firstIssued = firstIssued,
    )

private fun documentOf(
    vararg vulnerabilities: CycloneDxVexVulnerability,
    project: Project = dev.vulnlog.lib.codec.cyclonedx.project,
): CycloneDxVexDocument {
    val collection =
        CycloneDxVexCollection(
            CycloneDxVexScope(release("1.0.0")),
            vulnerabilities.toList(),
            listOf(product),
            emptyList(),
        )
    return buildCycloneDxVexDocument(project, identity, collection, tooling = VexTooling("CLI", "0.18.0"))
}

private fun dtoOf(vararg vulnerabilities: CycloneDxVexVulnerability): CycloneDxBomDto =
    CycloneDxVexEncoder.toDto(documentOf(*vulnerabilities))

private fun vulnerabilityDtoOf(status: VexStatus): CycloneDxVulnerabilityDto =
    dtoOf(vulnerability(status)).vulnerabilities.single()

class CycloneDxVexEncoderTest :
    FunSpec({

        context("analysis") {

            test("an open investigation is in triage, with the analysis as detail") {
                val dto = vulnerabilityDtoOf(VexStatus.UnderInvestigation("looking into it"))

                dto.analysis.state shouldBe "in_triage"
                dto.analysis.detail shouldBe "looking into it"
                dto.analysis.justification.shouldBeNull()
                dto.analysis.response.shouldBeNull()
                dto.recommendation.shouldBeNull()
            }

            test("a fix is resolved and carries no detail, because it would describe the state before the fix") {
                val dto = vulnerabilityDtoOf(VexStatus.Fixed)

                dto.analysis.state shouldBe "resolved"
                dto.analysis.detail.shouldBeNull()
                dto.analysis.response.shouldBeNull()
            }

            test("not affected maps each justification and keeps the OpenVEX token as a property") {
                val expected =
                    mapOf(
                        VexJustification.COMPONENT_NOT_PRESENT to "code_not_present",
                        VexJustification.VULNERABLE_CODE_NOT_PRESENT to "code_not_present",
                        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH to "code_not_reachable",
                        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY to "protected_at_runtime",
                        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST to "protected_by_mitigating_control",
                    )

                val dtos = expected.keys.associateWith { vulnerabilityDtoOf(VexStatus.NotAffected(it, "why")) }

                dtos.mapValues { (_, dto) -> dto.analysis.justification } shouldBe expected
                dtos.forEach { (justification, dto) ->
                    dto.analysis.state shouldBe "not_affected"
                    dto.analysis.detail shouldBe "why"
                    dto.properties shouldContainExactly
                        listOf(CycloneDxPropertyDto("vulnlog:justification", justification.name.lowercase()))
                }
            }

            test("affected is exploitable, with the remediation as response and recommendation") {
                val riskAcceptedWithFix = "The risk is accepted for this release. A fix ships with release 1.1.0."
                val expected =
                    mapOf(
                        Remediation.RiskAccepted(
                            null,
                        ) to ("will_not_fix" to "The risk is accepted. No fix is planned."),
                        Remediation.RiskAccepted(release("1.1.0")) to ("will_not_fix" to riskAcceptedWithFix),
                        Remediation.FixPlanned(null) to (null to "A fix is planned but not yet available."),
                        Remediation.FixPlanned(release("1.1.0")) to (null to "A fix is planned for release 1.1.0."),
                        Remediation.UpdateTo(release("1.1.0")) to ("update" to "Update to release 1.1.0."),
                        Remediation.NoneAvailable to ("can_not_fix" to "No remediation is available yet."),
                    )

                val dtos = expected.keys.associateWith { vulnerabilityDtoOf(VexStatus.Affected(it, "reachable")) }

                dtos.mapValues { (_, dto) -> dto.analysis.response?.single() to dto.recommendation } shouldBe expected
                dtos.values.forEach { dto ->
                    dto.analysis.state shouldBe "exploitable"
                    dto.analysis.detail shouldBe "reachable"
                }
            }
        }

        context("firstIssued") {

            test("a carried time is written as the baseline carries it") {
                val carried = VexStatementTime.Carried(Instant.parse("2026-04-20T12:34:56Z"))

                val dto = dtoOf(vulnerability(VexStatus.Fixed, carried)).vulnerabilities.single()

                dto.analysis.firstIssued shouldBe "2026-04-20T12:34:56Z"
            }

            test("an issued analysis is written with the document's time, so it never inherits a later one") {
                val dto = dtoOf(vulnerability(VexStatus.Fixed)).vulnerabilities.single()

                dto.analysis.firstIssued shouldBe "2026-04-25T00:00:00Z"
            }
        }

        context("components") {

            test("the packages of every vulnerability are listed once, sorted, and referenced by purl") {
                val dto =
                    dtoOf(
                        vulnerability(VexStatus.Fixed, packages = listOf(parser, lib)),
                        vulnerability(VexStatus.Fixed, id = "CVE-2026-2222", packages = listOf(lib)),
                    )

                dto.components.map { it.bomRef } shouldContainExactly listOf(lib.value, parser.value)
                dto.components.map { it.type to it.purl } shouldContainExactly
                    listOf("library" to lib.value, "library" to parser.value)
                dto.vulnerabilities.map { vulnerability -> vulnerability.affects.map { it.ref } } shouldContainExactly
                    listOf(listOf(parser.value, lib.value), listOf(lib.value))
            }

            test("a component is named after its purl, so a consumer without purl matching still identifies it") {
                val dto =
                    dtoOf(vulnerability(VexStatus.Fixed, packages = listOf(Purl.Maven("pkg:maven/com.acme/lib@2.0"))))

                val component = dto.components.single()

                component.group shouldBe "com.acme"
                component.name shouldBe "lib"
                component.version shouldBe "2.0"
            }

            test("the product is the release of the project, with the release purls as its parts") {
                val component = dtoOf(vulnerability(VexStatus.Fixed)).metadata.component

                component.type shouldBe "application"
                component.name shouldBe "Acme Web App"
                component.version shouldBe "1.0.0"
                component.bomRef.shouldBeNull()
                component.components?.map { it.type to it.purl } shouldBe listOf("application" to product.value)
            }
        }

        context("metadata") {

            test("an author with an address is a contact with an email") {
                val document =
                    documentOf(vulnerability(VexStatus.Fixed), project = project.copy(contact = "sec@acme.example"))

                val author =
                    CycloneDxVexEncoder
                        .toDto(document)
                        .metadata.authors
                        .single()

                author.name shouldBe "Acme Security Team"
                author.email shouldBe "sec@acme.example"
            }

            test("an author with another contact keeps it in the name, as OpenVEX writes it") {
                val document =
                    documentOf(
                        vulnerability(VexStatus.Fixed),
                        project = project.copy(contact = "https://acme.example/security"),
                    )

                val author =
                    CycloneDxVexEncoder
                        .toDto(document)
                        .metadata.authors
                        .single()

                author.name shouldBe "Acme Security Team (https://acme.example/security)"
                author.email.shouldBeNull()
            }

            test("the tool names Vulnlog, the platform and the version") {
                val tool =
                    dtoOf(vulnerability(VexStatus.Fixed))
                        .metadata.tools
                        ?.components
                        ?.single()

                tool?.name shouldBe "Vulnlog CLI"
                tool?.version shouldBe "0.18.0"
            }
        }

        test("a document with every status and an author without an address validates against the schema") {
            val document =
                documentOf(
                    vulnerability(VexStatus.UnderInvestigation("looking")),
                    vulnerability(VexStatus.Fixed, id = "CVE-2026-2222"),
                    vulnerability(VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT), id = "CVE-2026-3333"),
                    vulnerability(VexStatus.Affected(Remediation.FixPlanned(null)), id = "CVE-2026-4444"),
                    project = project.copy(contact = "https://acme.example/security"),
                )

            val errors =
                cycloneDxJsonSchema(CycloneDxVexFormatVersion.LATEST)
                    .validate(CycloneDxVexEncoder.encode(document), InputFormat.JSON)

            errors.shouldBeEmpty()
        }
    })
