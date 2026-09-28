// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import com.github.packageurl.PackageURL
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.carryOverOpenVexTimestamps
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
import dev.vulnlog.lib.core.vex.openvex.resolveOpenVexIdentity
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion.VERSION_0_2_0
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val DOCUMENT_ID = "https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79"
private val DOCUMENT_IRI = OpenVexDocumentId(DOCUMENT_ID)
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")

private fun document(
    context: String = "https://openvex.dev/ns/v0.2.0",
    id: String? = DOCUMENT_ID,
    timestamp: String? = "2026-04-25T00:00:00Z",
    version: Long? = 1,
): String {
    val fields =
        listOfNotNull(
            """"@context": "$context"""",
            id?.let { """"@id": "$it"""" },
            timestamp?.let { """"timestamp": "$it"""" },
            version?.let { """"version": $it""" },
        )
    return "{${fields.joinToString(", ")}}"
}

private fun read(content: String): OpenVexBaselineResult = parseOpenVexBaseline(content, VERSION_0_2_0)

private fun baselineOf(version: Int = 1): OpenVexBaseline =
    OpenVexBaseline(formatVersion = VERSION_0_2_0, id = DOCUMENT_IRI, version = OpenVexDocumentVersion(version))

private val fileWithOneStatement: VulnlogFile =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0")))),
        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1111"), releases = listOf(release("1.0.0")))),
    )

private fun firstDocumentOf(file: VulnlogFile): OpenVexDocument =
    buildOpenVexDocument(
        file.project,
        OpenVexIdentity(DOCUMENT_IRI, ISSUED_AT, OpenVexDocumentVersion.FIRST),
        collectOpenVexStatements(file).statements,
    )

private fun readBack(content: String): OpenVexBaseline =
    read(content).shouldBeInstanceOf<OpenVexBaselineResult.Parsed>().baseline

/** Continues [baseline] over [file] the way a run does. */
private fun revisionOf(
    file: VulnlogFile,
    baseline: OpenVexBaseline,
): OpenVexDocument =
    buildOpenVexDocument(
        file.project,
        resolveOpenVexIdentity(OpenVexRevision.Next(baseline), UPDATED_AT),
        carryOverOpenVexTimestamps(collectOpenVexStatements(file).statements, baseline),
    )

class OpenVexBaselineParserTest :
    FunSpec({

        context("parseOpenVexBaseline") {

            test("reads the identity of a document in the required format version") {
                val content = document(version = 3)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineResult.Parsed(baselineOf(version = 3))
            }

            test("a document without a version is the first revision") {
                val content = document(version = null)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineResult.Parsed(baselineOf(version = 1))
            }

            test("a timestamp with an offset still counts as a document") {
                val content = document(timestamp = "2026-04-25T02:00:00+02:00")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineResult.Parsed(baselineOf())
            }

            test("a document in another OpenVEX format version is not continued") {
                val content = document(context = "https://openvex.dev/ns/v0.1.0")

                val outcome = read(content)

                outcome shouldBe
                    OpenVexBaselineResult.Rejected(OpenVexBaselineProblem.OtherFormatVersion("0.1.0", VERSION_0_2_0))
            }

            test("a document with a foreign context is not OpenVEX") {
                val content = document(context = "https://cyclonedx.org/schema")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineResult.Rejected(OpenVexBaselineProblem.NotOpenVex)
            }

            test("an identity field that is missing or cannot be continued names the field and its value") {
                val contents =
                    listOf(
                        document(id = null),
                        document(id = "vex-1"),
                        document(timestamp = null),
                        document(timestamp = "yesterday"),
                        document(version = 0),
                        document(version = Int.MAX_VALUE.toLong()),
                    )

                val outcomes = contents.map(::read)

                outcomes shouldBe
                    listOf(
                        OpenVexIdentityField.ID to null,
                        OpenVexIdentityField.ID to "vex-1",
                        OpenVexIdentityField.TIMESTAMP to null,
                        OpenVexIdentityField.TIMESTAMP to "yesterday",
                        OpenVexIdentityField.VERSION to "0",
                        OpenVexIdentityField.VERSION to Int.MAX_VALUE.toString(),
                    ).map { (field, value) ->
                        OpenVexBaselineResult.Rejected(OpenVexBaselineProblem.InvalidIdentity(field, value))
                    }
            }

            test("malformed JSON is not a document") {
                val content = "{ not json"

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineResult.Rejected(OpenVexBaselineProblem.NotOpenVex)
            }
        }

        context("sameOpenVexContent") {

            test("a baseline that cannot be parsed is a change") {
                val document = firstDocumentOf(fileWithOneStatement)

                val unchanged = sameOpenVexContent("{ not json", document)

                unchanged shouldBe false
            }

            test("key order and formatting do not count as a change") {
                val reordered =
                    """{"statements": [{"supplier": "org", "status": "under_investigation", """ +
                        """"timestamp": "2026-04-25T00:00:00Z", """ +
                        """"products": [{"identifiers": {"purl": "pkg:maven/com.acme/app@1.0.0"}, """ +
                        """"@id": "pkg:maven/com.acme/app@1.0.0"}], """ +
                        """"vulnerability": {"name": "CVE-2026-1111", """ +
                        """"@id": "https://nvd.nist.gov/vuln/detail/CVE-2026-1111"}}], "version": 1, """ +
                        """"timestamp": "2026-04-25T00:00:00Z", "role": "Document Creator", "author": "author", """ +
                        """"@id": "$DOCUMENT_ID", "@context": "https://openvex.dev/ns/v0.2.0"}"""

                val unchanged = sameOpenVexContent(reordered, revisionOf(fileWithOneStatement, readBack(reordered)))

                unchanged shouldBe true
            }

            test("a field Vulnlog does not write is a change") {
                val foreign =
                    OpenVexEncoder
                        .encode(firstDocumentOf(fileWithOneStatement))
                        .replaceFirst("\"version\": 1,", "\"version\": 1,\n  \"tooling\": \"vexctl\",")

                val unchanged = sameOpenVexContent(foreign, revisionOf(fileWithOneStatement, readBack(foreign)))

                unchanged shouldBe false
            }
        }

        context("statements") {

            test("reads back every statement Vulnlog wrote, each with the time it carries") {
                val document = firstDocumentOf(everyStatusFile())

                val baseline = readBack(OpenVexEncoder.encode(document))

                baseline.statements shouldBe
                    document.statements.map { statement ->
                        val at =
                            when (val time = statement.timestamp) {
                                is OpenVexStatementTime.Stated -> time.date.atStartOfDay(ZoneOffset.UTC).toInstant()
                                is OpenVexStatementTime.Carried -> time.at
                                OpenVexStatementTime.Issued -> ISSUED_AT
                            }
                        statement.copy(timestamp = OpenVexStatementTime.Carried(at))
                    }
            }

            test("a statement without a timestamp carries the document's") {
                val content = withStatements(statement(timestamp = null))

                val statement = readBack(content).statements.single()

                statement.timestamp shouldBe OpenVexStatementTime.Carried(ISSUED_AT)
            }

            test("a statement Vulnlog cannot read is skipped, and the baseline still reads") {
                val content = withStatements(statement(status = "exploitable"), statement())

                val baseline = readBack(content)

                baseline.statements.map { it.status } shouldBe listOf(VexStatus.UnderInvestigation())
            }

            test("statements that are no array read as none") {
                val content = document().replace("}", ", \"statements\": {}}")

                val baseline = readBack(content)

                baseline.statements shouldBe emptyList()
            }
        }
    })

private fun statement(
    status: String = "under_investigation",
    timestamp: String? = "2026-04-20T08:30:00Z",
): String {
    val fields =
        listOfNotNull(
            """"vulnerability": {"name": "CVE-2026-1111"}""",
            timestamp?.let { """"timestamp": "$it"""" },
            """"products": [{"@id": "pkg:maven/com.acme/app@1.0.0"}]""",
            """"status": "$status"""",
        )
    return "{${fields.joinToString(", ")}}"
}

private fun withStatements(vararg statements: String): String =
    document().removeSuffix("}") + """, "statements": [${statements.joinToString(", ")}]}"""

/** One entry per status, with aliases, packages, analysis and an OCI purl, so every field makes the round trip. */
private fun everyStatusFile(): VulnlogFile {
    val oci = parsePurl(PackageURL("pkg:oci/app?repository_url=ghcr.io/acme/app&tag=1.0.0"))
    val releases =
        listOf(
            releaseEntry("1.0.0", purls = listOf(PurlEntry(oci), mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
            releaseEntry("1.1.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.1.0"))),
        )
    val packages = listOf(Purl.Npm("pkg:npm/example-lib@2.3.0"))
    return vulnlogFile(
        releases = releases,
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve("CVE-2026-1111"),
                    releases = listOf(release("1.0.0")),
                    packages = packages,
                    aliases = listOf(ghsa("GHSA-jfh8-c2jp-5v3q")),
                    description = "Remote code execution in example-lib",
                    analyzedAt = LocalDate.of(2026, 4, 6),
                    verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    analysis = "The JNDI code path is never reached.",
                ),
                vulnerability(
                    id = cve("CVE-2026-2222"),
                    releases = listOf(release("1.0.0")),
                    packages = packages,
                    verdict = Verdict.Affected(Severity.HIGH, Disposition.WILL_FIX),
                    resolution = resolution("1.1.0", note = "Updated example-lib."),
                    analysis = "Reachable from the upload endpoint.",
                ),
                vulnerability(id = ghsa("GHSA-xxxx-yyyy-zzzz"), releases = listOf(release("1.1.0"))),
            ),
    )
}
