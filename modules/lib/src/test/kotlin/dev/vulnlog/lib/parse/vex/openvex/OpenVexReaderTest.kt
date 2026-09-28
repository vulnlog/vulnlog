// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import com.github.packageurl.PackageURL
import dev.vulnlog.lib.core.parsePurl
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.carryOverOpenVexTimestamps
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
import dev.vulnlog.lib.core.vex.openvex.freshOpenVexIdentity
import dev.vulnlog.lib.core.vex.openvex.nextOpenVexIdentity
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
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion.VERSION_0_2_0
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentityField
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

/** A minimal document with the identity fields the reader looks at. */
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

/** Every test reads for the version this build writes. */
private fun read(content: String): OpenVexBaselineOutcome = OpenVexReader.readBaseline(content, VERSION_0_2_0)

private fun baselineOf(
    content: String,
    version: Int = 1,
): OpenVexBaseline =
    OpenVexBaseline(
        formatVersion = VERSION_0_2_0,
        id = DOCUMENT_IRI,
        version = OpenVexDocumentVersion(version),
        content = content,
    )

/** One entry affecting every release given, so each release contributes a statement. */
private fun fileWith(releases: List<String>): VulnlogFile =
    vulnlogFile(
        releases =
            releases.map { id ->
                releaseEntry(id, purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@$id")))
            },
        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1111"), releases = releases.map(::release))),
    )

private fun documentOf(
    file: VulnlogFile,
    identity: OpenVexIdentity,
): OpenVexDocument = buildOpenVexDocument(file.project, identity, collectOpenVexStatements(file).statements)

/** The baseline a later run reads back from [content]. */
private fun readBack(content: String): OpenVexBaseline =
    read(content).shouldBeInstanceOf<OpenVexBaselineOutcome.Read>().baseline

/** The revision continuing [baseline] over [file], with the times the baseline carries for untouched statements. */
private fun revisionOf(
    file: VulnlogFile,
    baseline: OpenVexBaseline,
): OpenVexDocument =
    buildOpenVexDocument(
        file.project,
        nextOpenVexIdentity(baseline, UPDATED_AT),
        carryOverOpenVexTimestamps(collectOpenVexStatements(file).statements, baseline),
    )

class OpenVexReaderTest :
    FunSpec({

        context("readBaseline") {

            test("reads the identity of a document in the required format version") {
                val content = document(version = 3)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.Read(baselineOf(content, version = 3))
            }

            test("a document without a version is the first revision") {
                val content = document(version = null)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.Read(baselineOf(content, version = 1))
            }

            test("a timestamp with an offset still counts as a document") {
                val content = document(timestamp = "2026-04-25T02:00:00+02:00")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.Read(baselineOf(content))
            }

            test("an older OpenVEX format version is not continued") {
                val content = document(context = "https://openvex.dev/ns/v0.1.0")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.OtherFormatVersion("0.1.0", VERSION_0_2_0)
            }

            test("an OpenVEX format version this build does not know is not continued") {
                val content = document(context = "https://openvex.dev/ns/v9.9.9")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.OtherFormatVersion("9.9.9", VERSION_0_2_0)
            }

            test("a foreign context is not a document") {
                val content = document(context = "https://cyclonedx.org/schema")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
            }

            test("a context with an empty version is not a document") {
                val content = document(context = "https://openvex.dev/ns/v")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
            }

            test("a context without a version is an OpenVEX 0.0.1 document, which is not continued") {
                val content = document(context = "https://openvex.dev/ns")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.OtherFormatVersion("0.0.1", VERSION_0_2_0)
            }

            test("a document without an identifier cannot be continued") {
                val content = document(id = null)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.ID, null)
            }

            test("an identifier that is no absolute IRI cannot be continued") {
                listOf("vex-1", " https://vulnlog.dev/vex/abc", "https://vulnlog.dev/vex/a b").forEach { id ->
                    val outcome = read(document(id = id))

                    outcome shouldBe OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.ID, id)
                }
            }

            test("a document without a timestamp cannot be continued") {
                val content = document(timestamp = null)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.TIMESTAMP, null)
            }

            test("a document with an unparsable timestamp cannot be continued") {
                val content = document(timestamp = "yesterday")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.TIMESTAMP, "yesterday")
            }

            test("a version below 1 cannot be continued") {
                listOf(0L, -3L).forEach { version ->
                    val outcome = read(document(version = version))

                    outcome shouldBe
                        OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.VERSION, version.toString())
                }
            }

            test("a version without a successor cannot be continued") {
                listOf(Int.MAX_VALUE.toLong(), Long.MAX_VALUE).forEach { version ->
                    val outcome = read(document(version = version))

                    outcome shouldBe
                        OpenVexBaselineOutcome.InvalidIdentity(OpenVexIdentityField.VERSION, version.toString())
                }
            }

            test("the last version with a successor is continued") {
                val content = document(version = Int.MAX_VALUE - 1L)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.Read(baselineOf(content, version = Int.MAX_VALUE - 1))
            }

            test("malformed JSON is not a document") {
                val content = "{ not json"

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
            }
        }

        context("isUnchanged") {

            test("a rerun over the same file changes nothing but the version and the clock") {
                val file = fileWith(listOf("1.0.0"))
                val first = documentOf(file, freshOpenVexIdentity(DOCUMENT_IRI, ISSUED_AT))
                val baseline = readBack(OpenVexWriter.write(first))

                val unchanged = OpenVexReader.isUnchanged(baseline, revisionOf(file, baseline))

                unchanged shouldBe true
            }

            test("an added statement is a change") {
                val first =
                    documentOf(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_IRI, ISSUED_AT))
                val baseline = readBack(OpenVexWriter.write(first))
                val grown = fileWith(listOf("1.0.0", "1.1.0"))

                val unchanged = OpenVexReader.isUnchanged(baseline, revisionOf(grown, baseline))

                unchanged shouldBe false
            }

            test("a baseline that cannot be parsed is a change") {
                val baseline = baselineOf("{ not json")
                val document =
                    documentOf(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_IRI, ISSUED_AT))

                val unchanged = OpenVexReader.isUnchanged(baseline, document)

                unchanged shouldBe false
            }

            test("key order and formatting do not count as a change") {
                val file = fileWith(listOf("1.0.0"))
                val reordered =
                    """{"statements": [{"supplier": "org", "status": "under_investigation", """ +
                        """"timestamp": "2026-04-25T00:00:00Z", """ +
                        """"products": [{"identifiers": {"purl": "pkg:maven/com.acme/app@1.0.0"}, """ +
                        """"@id": "pkg:maven/com.acme/app@1.0.0"}], """ +
                        """"vulnerability": {"name": "CVE-2026-1111", """ +
                        """"@id": "https://nvd.nist.gov/vuln/detail/CVE-2026-1111"}}], "version": 1, """ +
                        """"timestamp": "2026-04-25T00:00:00Z", "role": "Document Creator", "author": "author", """ +
                        """"@id": "$DOCUMENT_ID", "@context": "https://openvex.dev/ns/v0.2.0"}"""
                val baseline = readBack(reordered)

                val unchanged = OpenVexReader.isUnchanged(baseline, revisionOf(file, baseline))

                unchanged shouldBe true
            }

            test("a field this writer does not emit is a change") {
                val file = fileWith(listOf("1.0.0"))
                val first = documentOf(file, freshOpenVexIdentity(DOCUMENT_IRI, ISSUED_AT))
                val foreign =
                    OpenVexWriter
                        .write(
                            first,
                        ).replaceFirst("\"version\": 1,", "\"version\": 1,\n  \"tooling\": \"vexctl\",")
                val baseline = readBack(foreign)

                val unchanged = OpenVexReader.isUnchanged(baseline, revisionOf(file, baseline))

                unchanged shouldBe false
            }
        }

        context("statements") {

            test("reads back every statement this writer wrote, each with the time it carries") {
                val document = documentOf(everyStatusFile(), freshOpenVexIdentity(DOCUMENT_IRI, ISSUED_AT))

                val baseline = readBack(OpenVexWriter.write(document))

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

            test("a statement carries its own timestamp") {
                val statement = readBack(withStatements(statement())).statements.single()

                statement.timestamp shouldBe OpenVexStatementTime.Carried(Instant.parse("2026-04-20T08:30:00Z"))
            }

            test("a statement without a timestamp carries the document's") {
                val statement = readBack(withStatements(statement(timestamp = null))).statements.single()

                statement.timestamp shouldBe OpenVexStatementTime.Carried(ISSUED_AT)
            }

            test("a statement this writer cannot read is skipped, and the baseline still reads") {
                val content = withStatements(statement(status = "exploitable"), statement())

                val baseline = readBack(content)

                baseline.statements.map { it.status } shouldBe listOf(VexStatus.UnderInvestigation())
            }

            test("statements that are no array read as none") {
                val content = document().replace("}", ", \"statements\": {}}")

                readBack(content).statements shouldBe emptyList()
            }
        }
    })

/** A minimal statement, dated apart from the document unless [timestamp] is null. */
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

/** A minimal document making [statements]. */
private fun withStatements(vararg statements: String): String =
    document().removeSuffix("}") + """, "statements": [${statements.joinToString(", ")}]}"""

/** One entry per status, with aliases, packages and analysis, so every statement field is written and read back. */
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
