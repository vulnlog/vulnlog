// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
import dev.vulnlog.lib.core.vex.openvex.freshOpenVexIdentity
import dev.vulnlog.lib.core.vex.openvex.nextOpenVexIdentity
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion.VERSION_0_2_0
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private const val DOCUMENT_ID = "https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79"
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")

/** A minimal document with the identity fields the reader looks at. */
private fun document(
    context: String = "https://openvex.dev/ns/v0.2.0",
    id: String? = DOCUMENT_ID,
    timestamp: String? = "2026-04-25T00:00:00Z",
    version: Int? = 1,
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
    OpenVexBaseline(formatVersion = VERSION_0_2_0, id = DOCUMENT_ID, version = version, content = content)

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

            test("a context without a version is not a document") {
                val content = document(context = "https://openvex.dev/ns/v")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
            }

            test("a document without an identifier is not a document to continue") {
                val content = document(id = null)

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
            }

            test("a document with an unparsable timestamp is not a document to continue") {
                val content = document(timestamp = "yesterday")

                val outcome = read(content)

                outcome shouldBe OpenVexBaselineOutcome.NotADocument
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
                val first = documentOf(file, freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val baseline = baselineOf(OpenVexWriter.write(first))

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        documentOf(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe true
            }

            test("an added statement is a change") {
                val first =
                    documentOf(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val baseline = baselineOf(OpenVexWriter.write(first))
                val grown = fileWith(listOf("1.0.0", "1.1.0"))

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        documentOf(grown, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe false
            }

            test("a baseline that cannot be parsed is a change") {
                val baseline = baselineOf("{ not json")
                val document =
                    documentOf(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))

                val unchanged = OpenVexReader.isUnchanged(baseline, document)

                unchanged shouldBe false
            }

            test("key order and formatting do not count as a change") {
                val file = fileWith(listOf("1.0.0"))
                val reordered =
                    """{"statements": [{"supplier": "org", "status": "under_investigation", """ +
                        """"products": [{"identifiers": {"purl": "pkg:maven/com.acme/app@1.0.0"}, """ +
                        """"@id": "pkg:maven/com.acme/app@1.0.0"}], """ +
                        """"vulnerability": {"name": "CVE-2026-1111", """ +
                        """"@id": "https://nvd.nist.gov/vuln/detail/CVE-2026-1111"}}], "version": 1, """ +
                        """"timestamp": "2026-04-25T00:00:00Z", "role": "Document Creator", "author": "author", """ +
                        """"@id": "$DOCUMENT_ID", "@context": "https://openvex.dev/ns/v0.2.0"}"""
                val baseline = baselineOf(reordered)

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        documentOf(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe true
            }

            test("a field this writer does not emit is a change") {
                val file = fileWith(listOf("1.0.0"))
                val first = documentOf(file, freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val foreign =
                    OpenVexWriter
                        .write(
                            first,
                        ).replaceFirst("\"version\": 1,", "\"version\": 1,\n  \"tooling\": \"vexctl\",")
                val baseline = baselineOf(foreign)

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        documentOf(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe false
            }
        }
    })
