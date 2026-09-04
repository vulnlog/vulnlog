// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
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

/** One entry affecting every release given, so each release contributes a statement. */
private fun fileWith(releases: List<String>): VulnlogFile =
    vulnlogFile(
        releases =
            releases.map { id ->
                releaseEntry(id, purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@$id")))
            },
        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1111"), releases = releases.map(::release))),
    )

class OpenVexReaderTest :
    FunSpec({

        context("readBaseline") {

            test("reads the identity of an OpenVEX document") {
                val content = document(version = 3)

                val baseline = OpenVexReader.readBaseline(content)

                baseline?.id shouldBe DOCUMENT_ID
                baseline?.timestamp shouldBe ISSUED_AT
                baseline?.version shouldBe 3
                baseline?.content shouldBe content
            }

            test("a document without a version is the first revision") {
                val content = document(version = null)

                val baseline = OpenVexReader.readBaseline(content)

                baseline?.version shouldBe 1
            }

            test("an older OpenVEX namespace is still continued") {
                val content = document(context = "https://openvex.dev/ns/v0.1.0")

                val baseline = OpenVexReader.readBaseline(content)

                baseline?.id shouldBe DOCUMENT_ID
            }

            test("a timestamp with an offset is normalized to UTC") {
                val content = document(timestamp = "2026-04-25T02:00:00+02:00")

                val baseline = OpenVexReader.readBaseline(content)

                baseline?.timestamp shouldBe ISSUED_AT
            }

            test("a foreign context is not a baseline") {
                val content = document(context = "https://cyclonedx.org/schema")

                val baseline = OpenVexReader.readBaseline(content)

                baseline.shouldBeNull()
            }

            test("a document without an identifier is not a baseline") {
                val content = document(id = null)

                val baseline = OpenVexReader.readBaseline(content)

                baseline.shouldBeNull()
            }

            test("a document with an unparsable timestamp is not a baseline") {
                val content = document(timestamp = "yesterday")

                val baseline = OpenVexReader.readBaseline(content)

                baseline.shouldBeNull()
            }

            test("malformed JSON is not a baseline") {
                val content = "{ not json"

                val baseline = OpenVexReader.readBaseline(content)

                baseline.shouldBeNull()
            }
        }

        context("isUnchanged") {

            test("a rerun over the same file changes nothing but the version and the clock") {
                val file = fileWith(listOf("1.0.0"))
                val first = buildOpenVexDocument(file, freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val baseline =
                    OpenVexBaseline(DOCUMENT_ID, ISSUED_AT, version = 1, content = OpenVexWriter.write(first))

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        buildOpenVexDocument(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe true
            }

            test("an added statement is a change") {
                val first =
                    buildOpenVexDocument(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val baseline =
                    OpenVexBaseline(DOCUMENT_ID, ISSUED_AT, version = 1, content = OpenVexWriter.write(first))
                val grown = fileWith(listOf("1.0.0", "1.1.0"))

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        buildOpenVexDocument(grown, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe false
            }

            test("a baseline that cannot be parsed is a change") {
                val baseline = OpenVexBaseline(DOCUMENT_ID, ISSUED_AT, version = 1, content = "{ not json")
                val document =
                    buildOpenVexDocument(fileWith(listOf("1.0.0")), freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))

                val unchanged = OpenVexReader.isUnchanged(baseline, document)

                unchanged shouldBe false
            }

            test("key order and formatting do not count as a change") {
                val file = fileWith(listOf("1.0.0"))
                val reordered =
                    """{"statements": [{"status": "under_investigation", """ +
                        """"products": [{"@id": "pkg:maven/com.acme/app@1.0.0"}], """ +
                        """"vulnerability": {"name": "CVE-2026-1111"}}], "version": 1, """ +
                        """"timestamp": "2026-04-25T00:00:00Z", "author": "author", """ +
                        """"@id": "$DOCUMENT_ID", "@context": "https://openvex.dev/ns/v0.2.0"}"""
                val baseline = OpenVexBaseline(DOCUMENT_ID, ISSUED_AT, version = 1, content = reordered)

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        buildOpenVexDocument(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe true
            }

            test("a field this writer does not emit is a change") {
                val file = fileWith(listOf("1.0.0"))
                val first = buildOpenVexDocument(file, freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))
                val foreign =
                    OpenVexWriter
                        .write(
                            first,
                        ).replaceFirst("\"version\": 1,", "\"version\": 1,\n  \"tooling\": \"vexctl\",")
                val baseline = OpenVexBaseline(DOCUMENT_ID, ISSUED_AT, version = 1, content = foreign)

                val unchanged =
                    OpenVexReader.isUnchanged(
                        baseline,
                        buildOpenVexDocument(file, nextOpenVexIdentity(baseline, UPDATED_AT)),
                    )

                unchanged shouldBe false
            }
        }
    })
