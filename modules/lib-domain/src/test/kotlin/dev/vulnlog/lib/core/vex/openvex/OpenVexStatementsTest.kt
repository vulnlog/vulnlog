// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.vex.vexStatusKind
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReporterType
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
import dev.vulnlog.lib.model.vex.openvex.OpenVexReleaseScope
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

/** Tags every entry and release purl of the tests that are not about tags. */
private val APP = tag("app")

private fun appPurl(purl: String): PurlEntry = mavenPurlEntry(purl, tags = listOf(APP.value))

private fun published(
    id: String,
    at: LocalDate,
): ReleaseEntry = releaseEntry(id, purls = listOf(appPurl("pkg:maven/com.acme/app@$id")), publishedAt = at)

private val affectedInV1 =
    vulnerability(
        id = cve("CVE-2026-1234"),
        releases = listOf(release("1.0.0")),
        tags = listOf(APP),
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
                    publishedAt = LocalDate.of(2026, 1, 15),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1", tags = listOf("container"))),
                    publishedAt = LocalDate.of(2026, 2, 1),
                ),
            ),
        vulnerabilities = listOf(affectedInV1.copy(tags = listOf(tag("container"), tag("library")))),
    )

private val unpublishedFix =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls = listOf(appPurl("pkg:maven/com.acme/app@1.0.0")),
                    publishedAt = LocalDate.of(2026, 1, 15),
                ),
                releaseEntry("1.0.1", purls = listOf(appPurl("pkg:maven/com.acme/app@1.0.1"))),
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
                        releases =
                            listOf(
                                releaseEntry("1.0.0", publishedAt = LocalDate.of(2026, 1, 15)),
                                releaseEntry("1.0.1", publishedAt = LocalDate.of(2026, 2, 1)),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val collection = collectOpenVexStatements(file)

                collection.statements.shouldBeEmpty()
                collection.skippedReleases shouldContainExactly listOf(release("1.0.0"), release("1.0.1"))
            }

            test("reports why each left-out entry names no product") {
                val file =
                    taggedFile.copy(
                        releases = taggedFile.releases + releaseEntry("1.1.0", publishedAt = LocalDate.of(2026, 3, 1)),
                        vulnerabilities =
                            listOf(
                                vulnerability(id = cve("CVE-2026-0001")),
                                affectedInV1.copy(
                                    id = cve("CVE-2026-0002"),
                                    releases = listOf(release("1.1.0")),
                                    resolution = null,
                                ),
                                affectedInV1.copy(id = cve("CVE-2026-0003"), tags = emptyList()),
                                affectedInV1.copy(id = cve("CVE-2026-0004"), tags = listOf(tag("build"))),
                            ),
                    )

                val skipped = collectOpenVexStatements(file).skippedEntries

                skipped shouldContainExactly
                    listOf(
                        OpenVexSkippedEntry.NoRelease(cve("CVE-2026-0001")),
                        OpenVexSkippedEntry.NoAnchoredRelease(cve("CVE-2026-0002")),
                        OpenVexSkippedEntry.NoTags(cve("CVE-2026-0003")),
                        OpenVexSkippedEntry.NoMatchingReleasePurl(cve("CVE-2026-0004")),
                    )
            }

            test("an intermediate release the entry does not list gets its statement too") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                published("1.0.0", LocalDate.of(2026, 1, 15)),
                                published("1.0.5", LocalDate.of(2026, 1, 20)),
                                published("1.0.1", LocalDate.of(2026, 2, 1)),
                                published("1.1.0", LocalDate.of(2026, 3, 1)),
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
                val shared = appPurl("pkg:maven/com.acme/app")
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(shared), publishedAt = LocalDate.of(2026, 1, 15)),
                                releaseEntry("1.0.1", purls = listOf(shared), publishedAt = LocalDate.of(2026, 2, 1)),
                            ),
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve("CVE-2026-1234"),
                                    releases = listOf(release("1.0.0"), release("1.0.1")),
                                    reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 1, 20))),
                                    tags = listOf(APP),
                                ),
                            ),
                    )

                val statements = collectOpenVexStatements(file).statements

                statements shouldHaveSize 1
            }
        }

        context("tags") {

            test("an entry names only the release purls matching its tags") {
                val entry = affectedInV1.copy(tags = listOf(tag("container")))

                val statements = collectOpenVexStatements(taggedFile.copy(vulnerabilities = listOf(entry))).statements

                statements.flatMap { it.products }.map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/app@1.0.1")
            }
        }

        context("release scope") {

            test("an unpublished release gets no statement and is named, so an unshipped fix is not stated as fixed") {
                val collection = collectOpenVexStatements(unpublishedFix)

                collection.statements.map { vexStatusKind(it.status) } shouldContainExactly
                    listOf(VexStatusKind.AFFECTED)
                collection.unpublishedReleases shouldContainExactly listOf(release("1.0.1"))
            }

            test("an unpublished release without purls is not reported as missing purls") {
                val file =
                    vulnlogFile(
                        releases = listOf(published("1.0.0", LocalDate.of(2026, 1, 15)), releaseEntry("1.0.1")),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val collection = collectOpenVexStatements(file)

                collection.skippedReleases.shouldBeEmpty()
            }

            test("a named release gets its statement even without published_at") {
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.1")))

                val collection = collectOpenVexStatements(unpublishedFix, scope)

                collection.statements.single().status shouldBe VexStatus.Fixed
                collection.unpublishedReleases.shouldBeEmpty()
            }

            test("only a release in scope anchors a statement, and a fix outside it still drives the remediation") {
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.0")))

                val statement = collectOpenVexStatements(taggedFile, scope).statements.single()

                statement.products.map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/lib@1.0.0")
                statement.status.shouldBeInstanceOf<VexStatus.Affected>().remediation shouldBe
                    Remediation.UpdateTo(release("1.0.1"))
            }

            test("a release the entry does not list is covered by the range") {
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.1")))

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
                                releaseEntry("1.0.0", purls = listOf(appPurl("pkg:maven/com.acme/app@1.0.0"))),
                                releaseEntry("1.0.1"),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.0")))

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

            test("an entry not carrying the tag is left out, even where its other tag reaches the purl") {
                val shared = mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("container", "library"))
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(shared), publishedAt = LocalDate.of(2026, 1, 15)),
                            ),
                        vulnerabilities = listOf(affectedInV1.copy(tags = listOf(tag("library")))),
                    )

                val collection = collectOpenVexStatements(file, OpenVexScope(tags = setOf(tag("container"))))

                collection.statements.shouldBeEmpty()
                collection.skippedEntries.shouldBeEmpty()
            }
        }

        context("statement fields") {

            val anchored = listOf(published("1.0.0", LocalDate.of(2026, 1, 15)))

            fun statementOf(entry: VulnerabilityEntry) =
                collectOpenVexStatements(
                    vulnlogFile(releases = anchored, vulnerabilities = listOf(entry)),
                ).statements.single()

            test("carries the entry's aliases, description, packages and analysis, sorted where it matters") {
                val entry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.0.0")),
                        tags = listOf(APP),
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

            test("is left to the revision to date when neither the entry nor its named, unpublished release is dated") {
                val entry =
                    vulnerability(id = cve("CVE-2026-1234"), releases = listOf(release("1.0.0")), tags = listOf(APP))
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(appPurl("pkg:maven/com.acme/app@1.0.0"))),
                            ),
                        vulnerabilities = listOf(entry),
                    )
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.0")))

                val statement = collectOpenVexStatements(file, scope).statements.single()

                statement.timestamp shouldBe OpenVexStatementTime.Issued
            }
        }

        context("openVexEmptyReason") {

            test("asks for purls when no release declares any") {
                val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

                val reason = openVexEmptyReason(bare, collectOpenVexStatements(bare))

                reason shouldBe OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS
            }

            test("asks for publication dates when only unpublished releases declare purls") {
                val unpublished =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(appPurl("pkg:maven/com.acme/app@1.0.0"))),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val reason = openVexEmptyReason(unpublished, collectOpenVexStatements(unpublished))

                reason shouldBe OpenVexEmptyReason.NO_PUBLISHED_RELEASE_DECLARES_PURLS
            }

            test("asks for tags when an entry reaches release purls without matching tags") {
                val untagged = taggedFile.copy(vulnerabilities = listOf(affectedInV1.copy(tags = emptyList())))

                val reason = openVexEmptyReason(untagged, collectOpenVexStatements(untagged))

                reason shouldBe OpenVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS
            }

            test("blames the tag scope when one is active") {
                val collection = collectOpenVexStatements(taggedFile, OpenVexScope(tags = setOf(tag("binary"))))

                val reason = openVexEmptyReason(taggedFile, collection)

                reason shouldBe OpenVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE
            }

            test("blames the release scope when one is active") {
                val later = affectedInV1.copy(releases = listOf(release("1.0.1")), tags = listOf(tag("container")))
                val file = taggedFile.copy(vulnerabilities = listOf(later))
                val scope = OpenVexScope(release = OpenVexReleaseScope.Named(release("1.0.0")))
                val collection = collectOpenVexStatements(file, scope)

                val reason = openVexEmptyReason(file, collection)

                reason shouldBe OpenVexEmptyReason.NO_ENTRY_IN_RELEASE_SCOPE
            }

            test("blames the entries otherwise") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                published("1.0.0", LocalDate.of(2026, 1, 15)),
                                releaseEntry("1.0.1"),
                            ),
                        vulnerabilities = listOf(affectedInV1.copy(releases = listOf(release("1.0.1")))),
                    )

                val reason = openVexEmptyReason(file, collectOpenVexStatements(file))

                reason shouldBe OpenVexEmptyReason.NO_ENTRY_ON_ANCHORED_RELEASE
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
