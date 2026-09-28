// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatement
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

private val affectedInV1 =
    vulnerability(
        id = cve("CVE-2026-1234"),
        releases = listOf(release("1.0.0")),
        verdict = Verdict.Affected(Severity.HIGH),
        resolution = resolution(release = "1.0.1"),
    )

private val taggedFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("container")),
                            mavenPurlEntry("pkg:maven/com.acme/lib@1.0.0", tags = listOf("library")),
                        ),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1", tags = listOf("container"))),
                ),
            ),
        vulnerabilities = listOf(affectedInV1),
    )

class OpenVexStatementsTest :
    FunSpec({

        context("collectOpenVexStatements") {

            test("anchors name every release with purls and the purls it contributes") {
                val collection = collectOpenVexStatements(taggedFile)

                collection.anchors.keys.toList() shouldContainExactly listOf(release("1.0.0"), release("1.0.1"))
                collection.anchors.getValue(release("1.0.0")).map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/lib@1.0.0")
            }

            test("a release without purls produces no statement and is reported") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("1.0.0"), releaseEntry("1.0.1")),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val collection = collectOpenVexStatements(file)

                collection.statements.shouldBeEmpty()
                collection.skippedReleases shouldContainExactly listOf(release("1.0.0"), release("1.0.1"))
                collection.skippedEntries shouldContainExactly
                    listOf(OpenVexSkippedEntry.NoAnchoredRelease(cve("CVE-2026-1234")))
            }

            test("skipped releases keep the order the file declares them in") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0"),
                                releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1"))),
                                releaseEntry("1.1.0"),
                            ),
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve("CVE-2026-1234"),
                                    releases = listOf(release("1.1.0"), release("1.0.0"), release("1.0.1")),
                                ),
                            ),
                    )

                val collection = collectOpenVexStatements(file)

                collection.skippedReleases shouldContainExactly listOf(release("1.0.0"), release("1.1.0"))
            }

            test("an entry without releases is reported as such") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                            ),
                        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1234"))),
                    )

                val collection = collectOpenVexStatements(file)

                collection.skippedEntries shouldContainExactly
                    listOf(OpenVexSkippedEntry.NoRelease(cve("CVE-2026-1234")))
            }

            test("an intermediate release the entry does not list gets its statement too") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                                releaseEntry("1.0.5", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.5"))),
                                releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1"))),
                                releaseEntry("1.1.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.1.0"))),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val statements = collectOpenVexStatements(file).statements

                statements.associate { it.products.single().value to vexStatusKind(it.status) } shouldBe
                    mapOf(
                        "pkg:maven/com.acme/app@1.0.0" to VexStatusKind.AFFECTED,
                        "pkg:maven/com.acme/app@1.0.5" to VexStatusKind.AFFECTED,
                        "pkg:maven/com.acme/app@1.0.1" to VexStatusKind.FIXED,
                        "pkg:maven/com.acme/app@1.1.0" to VexStatusKind.FIXED,
                    )
            }

            test("identical statements across releases collapse into one") {
                val shared = mavenPurlEntry("pkg:maven/com.acme/app")
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(shared)),
                                releaseEntry("1.0.1", purls = listOf(shared)),
                            ),
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve("CVE-2026-1234"),
                                    releases = listOf(release("1.0.0"), release("1.0.1")),
                                ),
                            ),
                    )

                val statements = collectOpenVexStatements(file).statements

                statements shouldHaveSize 1
            }
        }

        context("release scope") {

            test("only a release in scope anchors a statement") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.single().products.map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/lib@1.0.0")
            }

            test("a fix outside the scope still drives the remediation") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                val status = statements.single().status.shouldBeInstanceOf<VexStatus.Affected>()
                status.remediation shouldBe Remediation.UpdateTo(release("1.0.1"))
            }

            test("a release the entry does not list is covered by the range") {
                val scope = OpenVexScope(releases = setOf(release("1.0.1")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.single().products.map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.1")
                statements.single().status shouldBe VexStatus.Fixed
            }

            test("a release outside the scope is not reported as missing purls") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                                releaseEntry("1.0.1"),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val collection = collectOpenVexStatements(file, scope)

                collection.skippedReleases.shouldBeEmpty()
            }
        }

        context("tag scope") {

            test("only a purl carrying the tag becomes a product") {
                val scope = OpenVexScope(tags = setOf(tag("container")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.map { statement -> statement.products.single().value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/app@1.0.1")
            }

            test("a release left without purls by the tag is reported as missing purls") {
                val scope = OpenVexScope(tags = setOf(tag("library")))

                val collection = collectOpenVexStatements(taggedFile, scope)

                collection.skippedReleases shouldContainExactly listOf(release("1.0.1"))
            }

            test("a tag no purl carries produces no statement") {
                val scope = OpenVexScope(tags = setOf(tag("binary")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.shouldBeEmpty()
            }
        }

        context("statement fields") {

            val anchored = listOf(releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))))

            fun statementOf(entry: VulnerabilityEntry) =
                collectOpenVexStatements(
                    vulnlogFile(releases = anchored, vulnerabilities = listOf(entry)),
                ).statements.single()

            test("carries the entry's aliases, description, packages and analysis, sorted where it matters") {
                val entry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.0.0")),
                        aliases = listOf(ghsa("GHSA-zzzz-zzzz-zzzz"), ghsa("GHSA-aaaa-aaaa-aaaa")),
                        description = "Remote code execution",
                        packages = listOf(Purl.Npm("pkg:npm/zlib@1.0.0"), Purl.Npm("pkg:npm/alib@1.0.0")),
                        analysis = "not reachable",
                    )

                val statement = statementOf(entry)

                statement.vulnerability.aliases.map { it.id } shouldContainExactly
                    listOf("GHSA-aaaa-aaaa-aaaa", "GHSA-zzzz-zzzz-zzzz")
                statement.vulnerability.description shouldBe "Remote code execution"
                statement.subcomponents.map { it.value } shouldContainExactly
                    listOf("pkg:npm/alib@1.0.0", "pkg:npm/zlib@1.0.0")
                statement.status shouldBe VexStatus.UnderInvestigation("not reachable")
            }

            test("is left to the revision to date when neither the entry nor its release is dated") {
                val entry = vulnerability(id = cve("CVE-2026-1234"), releases = listOf(release("1.0.0")))

                val statement = statementOf(entry)

                statement.timestamp shouldBe OpenVexStatementTime.Issued
            }
        }

        context("openVexEmptyReason") {

            test("asks for purls when no release declares any") {
                val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

                openVexEmptyReason(bare, OpenVexScope()) shouldBe OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS
            }

            test("blames the tag scope when one is active") {
                openVexEmptyReason(taggedFile, OpenVexScope(tags = setOf(tag("binary")))) shouldBe
                    OpenVexEmptyReason.NO_PURL_CARRIES_TAG
            }

            test("blames the release scope when one is active") {
                openVexEmptyReason(taggedFile, OpenVexScope(releases = setOf(release("1.0.1")))) shouldBe
                    OpenVexEmptyReason.NO_ENTRY_IN_RELEASE_SCOPE
            }

            test("blames the entries otherwise") {
                openVexEmptyReason(taggedFile, OpenVexScope()) shouldBe OpenVexEmptyReason.NO_ENTRY_ON_ANCHORED_RELEASE
            }
        }

        context("carryOverOpenVexTimestamps") {
            val undated =
                OpenVexStatement(
                    vulnerability =
                        OpenVexVulnerability(
                            cve("CVE-2026-1234"),
                            aliases = emptyList(),
                            description = null,
                        ),
                    timestamp = OpenVexStatementTime.Issued,
                    products = listOf(Purl.Maven("pkg:maven/com.acme/app@1.0.0")),
                    subcomponents = emptyList(),
                    status = VexStatus.UnderInvestigation(),
                )
            val carried = OpenVexStatementTime.Carried(Instant.parse("2026-04-20T08:30:00Z"))

            fun baselineWith(vararg statements: OpenVexStatement): OpenVexBaseline =
                OpenVexBaseline(
                    formatVersion = OpenVexFormatVersion.LATEST,
                    id = OpenVexDocumentId("https://vulnlog.dev/vex/abc"),
                    version = OpenVexDocumentVersion.FIRST,
                    statements = statements.toList(),
                )

            test("an undated statement keeps the time the baseline carries for it") {
                val statements =
                    carryOverOpenVexTimestamps(listOf(undated), baselineWith(undated.copy(timestamp = carried)))

                statements shouldContainExactly listOf(undated.copy(timestamp = carried))
            }

            test("a changed statement is issued by the revision") {
                val changed = undated.copy(status = VexStatus.UnderInvestigation("Reachable after all."))

                val statements =
                    carryOverOpenVexTimestamps(listOf(changed), baselineWith(undated.copy(timestamp = carried)))

                statements shouldContainExactly listOf(changed)
            }

            test("a date the file states wins over the baseline") {
                val stated = undated.copy(timestamp = OpenVexStatementTime.Stated(LocalDate.of(2026, 4, 6)))

                val statements =
                    carryOverOpenVexTimestamps(listOf(stated), baselineWith(undated.copy(timestamp = carried)))

                statements shouldContainExactly listOf(stated)
            }

            test("of equal statements in the baseline the earliest time is carried") {
                val later = OpenVexStatementTime.Carried(Instant.parse("2026-04-22T00:00:00Z"))
                val baseline = baselineWith(undated.copy(timestamp = later), undated.copy(timestamp = carried))

                val statements = carryOverOpenVexTimestamps(listOf(undated), baseline)

                statements shouldContainExactly listOf(undated.copy(timestamp = carried))
            }

            test("without a baseline nothing is carried") {
                carryOverOpenVexTimestamps(listOf(undated), baseline = null) shouldContainExactly listOf(undated)
            }
        }
    })
