// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import dev.vulnlog.lib.core.vex.cyclonedx.buildCycloneDxVexDocument
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexDocument
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentityField
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.time.LocalDate

private val V17 = CycloneDxVexFormatVersion.VERSION_1_7
private val SERIAL_NUMBER = CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79")
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val identity = CycloneDxVexIdentity(SERIAL_NUMBER, ISSUED_AT, VexDocumentVersion.FIRST)
private val project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@acme.example")
private val lib = Purl.Npm("pkg:npm/example-lib@2.3.0")
private val parser = Purl.Npm("pkg:npm/example-parser@1.0.0")

private fun vulnerability(
    id: String,
    status: VexStatus,
    firstIssued: VexStatementTime = VexStatementTime.Issued,
    packages: List<Purl> = listOf(lib),
): CycloneDxVexVulnerability =
    CycloneDxVexVulnerability(
        id = cve(id),
        aliases = listOf(ghsa("GHSA-jfh8-c2jp-5v3q")),
        description = "Remote code execution",
        packages = packages,
        status = status,
        firstIssued = firstIssued,
    )

private val everyStatus =
    listOf(
        vulnerability(
            "CVE-2026-1111",
            VexStatus.UnderInvestigation("looking"),
            VexStatementTime.Stated(LocalDate.of(2026, 4, 1)),
        ),
        vulnerability(
            "CVE-2026-2222",
            VexStatus.Fixed,
            VexStatementTime.Carried(Instant.parse("2026-04-20T12:00:00Z")),
        ),
        vulnerability("CVE-2026-3333", VexStatus.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT, "stripped")),
        vulnerability(
            "CVE-2026-4444",
            VexStatus.Affected(Remediation.FixPlanned(release("1.1.0")), "reachable"),
            packages = listOf(lib, parser),
        ),
    )

private fun documentOf(vararg vulnerabilities: CycloneDxVexVulnerability): CycloneDxVexDocument {
    val collection =
        CycloneDxVexCollection(
            CycloneDxVexScope(release("1.0.0")),
            vulnerabilities.toList(),
            listOf(Purl.Maven("pkg:maven/com.acme/app@1.0.0")),
            emptyList(),
        )
    return buildCycloneDxVexDocument(project, identity, collection)
}

private fun bom(
    serialNumber: String? = SERIAL_NUMBER.value,
    timestamp: String? = "2026-04-25T00:00:00Z",
    version: String? = "3",
    specVersion: String? = "1.7",
    bomFormat: String? = "CycloneDX",
): String {
    val fields =
        listOfNotNull(
            bomFormat?.let { """"bomFormat": "$it"""" },
            specVersion?.let { """"specVersion": "$it"""" },
            serialNumber?.let { """"serialNumber": "$it"""" },
            version?.let { """"version": $it""" },
            timestamp?.let { """"metadata": {"timestamp": "$it"}""" },
        )
    return fields.joinToString(", ", prefix = "{", postfix = "}")
}

private fun parsed(content: String): CycloneDxVexBaseline =
    parseCycloneDxVexBaseline(content, V17).shouldBeInstanceOf<CycloneDxVexBaselineResult.Parsed>().baseline

private fun rejected(content: String): CycloneDxVexBaselineProblem =
    parseCycloneDxVexBaseline(content, V17).shouldBeInstanceOf<CycloneDxVexBaselineResult.Rejected>().problem

class CycloneDxVexBaselineParserTest :
    FunSpec({

        context("parseCycloneDxVexBaseline") {

            test("reads the identity of a document in the required format version") {
                val baseline = parsed(bom())

                baseline shouldBe CycloneDxVexBaseline(V17, SERIAL_NUMBER, VexDocumentVersion(3))
            }

            test("a document without a version is the first revision") {
                parsed(bom(version = null)).version shouldBe VexDocumentVersion.FIRST
            }

            test("a timestamp with an offset still counts as a document") {
                parsed(bom(timestamp = "2026-04-25T02:00:00+02:00")).version shouldBe VexDocumentVersion(3)
            }

            test("a document in another CycloneDX version is not continued") {
                rejected(bom(specVersion = "1.6")) shouldBe CycloneDxVexBaselineProblem.OtherFormatVersion("1.6", V17)
            }

            test("a document of another format is not CycloneDX") {
                val foreign =
                    listOf(
                        """{"@context": "https://openvex.dev/ns/v0.2.0"}""",
                        bom(bomFormat = null),
                        bom(specVersion = null),
                    )

                foreign.map(::rejected).forEach { it shouldBe CycloneDxVexBaselineProblem.NotCycloneDx }
            }

            test("an identity field that is missing or cannot be continued names the field and its value") {
                val cases =
                    mapOf(
                        bom(serialNumber = null) to (CycloneDxVexIdentityField.SERIAL_NUMBER to null),
                        bom(serialNumber = "3e671687") to (CycloneDxVexIdentityField.SERIAL_NUMBER to "3e671687"),
                        bom(timestamp = null) to (CycloneDxVexIdentityField.TIMESTAMP to null),
                        bom(timestamp = "yesterday") to (CycloneDxVexIdentityField.TIMESTAMP to "yesterday"),
                        bom(version = "0") to (CycloneDxVexIdentityField.VERSION to "0"),
                        bom(version = Int.MAX_VALUE.toString()) to
                            (CycloneDxVexIdentityField.VERSION to Int.MAX_VALUE.toString()),
                    )

                cases.forEach { (content, expected) ->
                    rejected(content) shouldBe
                        CycloneDxVexBaselineProblem.InvalidIdentity(expected.first, expected.second)
                }
            }

            test("malformed JSON is not a document") {
                rejected("{") shouldBe CycloneDxVexBaselineProblem.NotCycloneDx
            }
        }

        context("sameCycloneDxVexContent") {

            test("a baseline that cannot be parsed is a change") {
                sameCycloneDxVexContent("{", documentOf(everyStatus.first())) shouldBe false
            }

            test("the version, the timestamp, key order and formatting do not count as a change") {
                val document = documentOf(everyStatus.first())
                val laterIdentity =
                    identity.copy(
                        timestamp = ISSUED_AT.plusSeconds(3600),
                        version = VexDocumentVersion(2),
                    )
                val later = CycloneDxVexEncoder.encode(document.copy(identity = laterIdentity))

                sameCycloneDxVexContent(reordered(later), document) shouldBe true
            }

            test("a field Vulnlog does not write is a change") {
                val document = documentOf(everyStatus.first())
                val foreign =
                    encoded(
                        everyStatus.first(),
                    ).replaceFirst("\"bomFormat\"", "\"compositions\": [], \"bomFormat\"")

                sameCycloneDxVexContent(foreign, document) shouldBe false
            }
        }

        context("vulnerabilities") {

            test("reads back every vulnerability Vulnlog wrote, each with the time it carries") {
                val document = documentOf(*everyStatus.toTypedArray())

                val baseline = parsed(CycloneDxVexEncoder.encode(document))

                baseline.vulnerabilities shouldContainExactly
                    listOf(
                        everyStatus[0].copy(
                            firstIssued = VexStatementTime.Carried(Instant.parse("2026-04-01T00:00:00Z")),
                        ),
                        everyStatus[1],
                        everyStatus[2].copy(firstIssued = VexStatementTime.Carried(ISSUED_AT)),
                        everyStatus[3].copy(firstIssued = VexStatementTime.Carried(ISSUED_AT)),
                    )
            }

            test("an analysis without firstIssued carries the document's time") {
                val content = encoded(everyStatus[1]).replace(Regex(""",\s*"firstIssued": "[^"]+""""), "")

                parsed(content).vulnerabilities.single().firstIssued shouldBe VexStatementTime.Carried(ISSUED_AT)
            }

            test("a vulnerability Vulnlog cannot read is skipped, and the baseline still reads") {
                val content =
                    encoded(
                        everyStatus[1],
                        everyStatus[2],
                    ).replace("\"vulnlog:justification\"", "\"other:justification\"")

                parsed(content).vulnerabilities.map { it.id } shouldContainExactly listOf(cve("CVE-2026-2222"))
            }

            test("an affected package the components do not list cannot be read back") {
                val content = encoded(everyStatus[1]).replace("\"bom-ref\": \"${lib.value}\"", "\"bom-ref\": \"lib\"")

                parsed(content).vulnerabilities shouldContainExactly emptyList()
            }

            test("vulnerabilities that are no array read as none") {
                parsed(bom().replaceFirst("}", "}, \"vulnerabilities\": {}")).vulnerabilities shouldContainExactly
                    emptyList()
            }
        }
    })

private fun encoded(vararg vulnerabilities: CycloneDxVexVulnerability): String =
    CycloneDxVexEncoder.encode(documentOf(*vulnerabilities))

/** Re-serialised compactly with the keys of every object sorted, as another tool might write it. */
private fun reordered(content: String): String {
    val mapper = JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build()
    return mapper.writeValueAsString(mapper.readValue(content, Map::class.java))
}
