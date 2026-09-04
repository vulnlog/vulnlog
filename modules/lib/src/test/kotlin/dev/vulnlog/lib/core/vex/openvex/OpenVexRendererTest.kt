// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

private const val DOCUMENT_ID = "https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79"
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")

/** One anchoring release, one without purls, and one entry per way of being left out. */
private val file =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                releaseEntry("1.0.1"),
            ),
        vulnerabilities =
            listOf(
                vulnerability(id = cve("CVE-2026-1111"), releases = listOf(release("1.0.0"))),
                vulnerability(id = cve("CVE-2026-2222"), releases = listOf(release("1.0.1"))),
                vulnerability(id = cve("CVE-2026-3333")),
            ),
    )

/** Two releases whose purls carry different tags, so a tag scope strips one release bare. */
private val taggedFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("container"))),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/lib@1.0.1", tags = listOf("library"))),
                ),
            ),
        vulnerabilities =
            listOf(vulnerability(id = cve("CVE-2026-1111"), releases = listOf(release("1.0.0"), release("1.0.1")))),
    )

class OpenVexRendererTest :
    FunSpec({

        context("renderOpenVexScope") {

            test("states what each active dimension resolved to") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")), tags = setOf(tag("container")))

                renderOpenVexScope(scope) shouldContainExactly
                    listOf("as-of scope expanded to releases: 1.0.0", "tag scope matched tags: container")
            }

            test("is silent without a scope") {
                renderOpenVexScope(OpenVexScope()) shouldContainExactly emptyList()
            }
        }

        context("renderOpenVexProducts") {

            test("names each anchoring release with its purl count") {
                val line = renderOpenVexProducts(collectOpenVexStatements(file))

                line shouldBe "anchored on 1 release with purls: '1.0.0' (1 purl)"
            }

            test("is silent when no release anchors") {
                val line = renderOpenVexProducts(collectOpenVexStatements(vulnlogFile()))

                line.shouldBeNull()
            }
        }

        context("renderOpenVexSkippedReleases") {

            test("names the releases without purls") {
                val line = renderOpenVexSkippedReleases(collectOpenVexStatements(file))

                line shouldBe "releases without purls are not part of the document: '1.0.1'"
            }

            test("names the scope when a tag stripped the release bare") {
                val scope = OpenVexScope(tags = setOf(tag("container")))

                val line = renderOpenVexSkippedReleases(collectOpenVexStatements(taggedFile, scope))

                line shouldBe "releases without purls in scope are not part of the document: '1.0.1'"
            }

            test("is silent when every release anchors") {
                val line = renderOpenVexSkippedReleases(collectOpenVexStatements(taggedFile))

                line.shouldBeNull()
            }
        }

        context("renderOpenVexSkippedEntries") {

            test("states why each entry is missing, sorted by id") {
                val lines = renderOpenVexSkippedEntries(collectOpenVexStatements(file))

                lines shouldContainExactly
                    listOf(
                        "skipped CVE-2026-2222: no release it applies to declares purls in scope",
                        "skipped CVE-2026-3333: it references no release",
                    )
            }
        }

        context("document lines") {

            val document = buildOpenVexDocument(file, freshOpenVexIdentity(DOCUMENT_ID, ISSUED_AT))

            test("renderOpenVexStatementCounts breaks the total down by status") {
                renderOpenVexStatementCounts(document) shouldBe "collected 1 statement: 1 under_investigation"
            }

            test("renderOpenVexWritten names the target, the format and the version") {
                renderOpenVexWritten("vex.json", document) shouldBe
                    "wrote vex.json: openvex format, version 1, 1 statement"
            }
        }

        context("renderOpenVexEmptyHint") {

            test("asks for purls when no release declares any") {
                val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

                renderOpenVexEmptyHint(bare, OpenVexScope()) shouldBe
                    "declare 'purls' on the releases you want the document to cover"
            }

            test("blames the tag scope when one is active") {
                renderOpenVexEmptyHint(file, OpenVexScope(tags = setOf(tag("binary")))) shouldBe
                    "no release purl in scope carries one of the requested tags"
            }

            test("blames the release scope when one is active") {
                renderOpenVexEmptyHint(file, OpenVexScope(releases = setOf(release("1.0.1")))) shouldBe
                    "no vulnerability entry references a release in scope that declares purls"
            }

            test("blames the entries otherwise") {
                renderOpenVexEmptyHint(file, OpenVexScope()) shouldBe
                    "no vulnerability entry references a release that declares purls"
            }
        }
    })
