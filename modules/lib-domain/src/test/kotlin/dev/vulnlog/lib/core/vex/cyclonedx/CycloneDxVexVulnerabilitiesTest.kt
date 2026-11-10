// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.cyclonedx

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
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexSkippedEntry
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

private val appPurl100 = Purl.Maven("pkg:maven/com.acme/app@1.0.0")
private val cliPurl100 = Purl.Maven("pkg:maven/com.acme/cli@1.0.0")
private val appPurl101 = Purl.Maven("pkg:maven/com.acme/app@1.0.1")
private val libPurl = Purl.Npm("pkg:npm/example-lib@2.3.0")
private val parserPurl = Purl.Npm("pkg:npm/example-parser@1.0.0")

private val notAffected =
    vulnerability(
        id = cve("CVE-2026-1111"),
        tags = listOf(tag("app")),
        aliases = listOf(ghsa("GHSA-jfh8-c2jp-5v3q")),
        releases = listOf(release("1.0.0")),
        description = "Remote code execution in example-lib",
        packages = listOf(libPurl),
        analyzedAt = LocalDate.of(2026, 4, 6),
        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
        analysis = "The JNDI code path is never reached.",
    )

private val fixedLater =
    vulnerability(
        id = cve("CVE-2026-2222"),
        tags = listOf(tag("app"), tag("cli")),
        releases = listOf(release("1.0.0")),
        packages = listOf(parserPurl),
        reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 7))),
        verdict = Verdict.Affected(Severity.CRITICAL),
        resolution = resolution(release = "1.0.1", at = LocalDate.of(2026, 4, 20)),
    )

private val laterOnly =
    vulnerability(
        id = cve("CVE-2026-3333"),
        tags = listOf(tag("app")),
        releases = listOf(release("1.0.1")),
        packages = listOf(libPurl),
        reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 18))),
    )

private fun fileWith(vararg vulnerabilities: VulnerabilityEntry): VulnlogFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry(appPurl100.value, tags = listOf("app")),
                            mavenPurlEntry(cliPurl100.value, tags = listOf("cli")),
                        ),
                    publishedAt = LocalDate.of(2026, 3, 2),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry(appPurl101.value, tags = listOf("app"))),
                    publishedAt = LocalDate.of(2026, 4, 21),
                ),
            ),
        vulnerabilities = vulnerabilities.toList(),
    )

private val file = fileWith(notAffected, fixedLater, laterOnly)

private fun scope(
    release: String,
    vararg tags: String,
): CycloneDxVexScope = CycloneDxVexScope(release(release), tags.map(::tag).toSet())

class CycloneDxVexVulnerabilitiesTest :
    FunSpec({

        context("collectCycloneDxVexVulnerabilities") {

            test("states each entry as it stands on the release in scope, sorted by id") {
                val collection = collectCycloneDxVexVulnerabilities(file, scope("1.0.0"))

                collection.productPurls shouldContainExactly listOf(appPurl100, cliPurl100)
                collection.vulnerabilities shouldContainExactly
                    listOf(
                        CycloneDxVexVulnerability(
                            id = cve("CVE-2026-1111"),
                            aliases = listOf(ghsa("GHSA-jfh8-c2jp-5v3q")),
                            description = "Remote code execution in example-lib",
                            packages = listOf(libPurl),
                            status =
                                VexStatus.NotAffected(
                                    VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH,
                                    "The JNDI code path is never reached.",
                                ),
                            firstIssued = VexStatementTime.Stated(LocalDate.of(2026, 4, 6)),
                        ),
                        CycloneDxVexVulnerability(
                            id = cve("CVE-2026-2222"),
                            aliases = emptyList(),
                            description = null,
                            packages = listOf(parserPurl),
                            status = VexStatus.Affected(Remediation.UpdateTo(release("1.0.1"))),
                            firstIssued = VexStatementTime.Stated(LocalDate.of(2026, 4, 7)),
                        ),
                    )
                collection.skippedEntries shouldContainExactly
                    listOf(CycloneDxVexSkippedEntry.NotOnRelease(cve("CVE-2026-3333")))
            }

            test("a later release carries the earlier entries on, fixed from the resolution release") {
                val collection = collectCycloneDxVexVulnerabilities(file, scope("1.0.1"))

                collection.vulnerabilities.map { it.id to it.status } shouldContainExactly
                    listOf(
                        cve("CVE-2026-1111") to
                            VexStatus.NotAffected(
                                VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH,
                                "The JNDI code path is never reached.",
                            ),
                        cve("CVE-2026-2222") to VexStatus.Fixed,
                        cve("CVE-2026-3333") to VexStatus.UnderInvestigation(),
                    )
                collection.vulnerabilities.map { it.firstIssued } shouldContainExactly
                    listOf(
                        VexStatementTime.Stated(LocalDate.of(2026, 4, 6)),
                        VexStatementTime.Stated(LocalDate.of(2026, 4, 20)),
                        VexStatementTime.Stated(LocalDate.of(2026, 4, 18)),
                    )
            }

            test("a tag keeps only the release purls and entries carrying it") {
                val collection = collectCycloneDxVexVulnerabilities(file, scope("1.0.0", "cli"))

                collection.productPurls shouldContainExactly listOf(cliPurl100)
                collection.vulnerabilities.map { it.id } shouldContainExactly listOf(cve("CVE-2026-2222"))
                collection.skippedEntries.shouldBeEmpty()
            }

            test("an entry without a tag shared with a release purl is skipped and says why") {
                val untagged = notAffected.copy(tags = emptyList())
                val mismatched = fixedLater.copy(tags = listOf(tag("docs")))
                val unreleased = laterOnly.copy(releases = emptyList())

                val collection =
                    collectCycloneDxVexVulnerabilities(fileWith(untagged, mismatched, unreleased), scope("1.0.0"))

                collection.vulnerabilities.shouldBeEmpty()
                collection.skippedEntries shouldContainExactly
                    listOf(
                        CycloneDxVexSkippedEntry.NoTags(cve("CVE-2026-1111")),
                        CycloneDxVexSkippedEntry.NoMatchingReleasePurl(cve("CVE-2026-2222")),
                        CycloneDxVexSkippedEntry.NoRelease(cve("CVE-2026-3333")),
                    )
            }

            test("an undated status on an unpublished release is issued with the document") {
                val unpublished =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry(
                                    "1.0.0",
                                    purls = listOf(mavenPurlEntry(appPurl100.value, tags = listOf("app"))),
                                ),
                            ),
                        vulnerabilities =
                            listOf(
                                laterOnly.copy(releases = listOf(release("1.0.0")), reports = emptyList()),
                            ),
                    )

                val collection = collectCycloneDxVexVulnerabilities(unpublished, scope("1.0.0"))

                collection.vulnerabilities.single().firstIssued shouldBe VexStatementTime.Issued
            }
        }

        context("cycloneDxVexEmptyReason") {

            test("blames the release without purls first") {
                val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")), vulnerabilities = listOf(notAffected))

                val reason = cycloneDxVexEmptyReason(bare, collectCycloneDxVexVulnerabilities(bare, scope("1.0.0")))

                reason shouldBe CycloneDxVexEmptyReason.RELEASE_DECLARES_NO_PURLS
            }

            test("blames the tag scope that leaves no release purl") {
                val collection = collectCycloneDxVexVulnerabilities(file, scope("1.0.1", "cli"))

                cycloneDxVexEmptyReason(file, collection) shouldBe CycloneDxVexEmptyReason.NO_RELEASE_PURL_IN_TAG_SCOPE
            }

            test("blames the entry tags when none match a release purl") {
                val mismatched = fileWith(notAffected.copy(tags = listOf(tag("docs"))))

                val reason =
                    cycloneDxVexEmptyReason(mismatched, collectCycloneDxVexVulnerabilities(mismatched, scope("1.0.0")))

                reason shouldBe CycloneDxVexEmptyReason.NO_ENTRY_MATCHES_RELEASE_PURL_TAGS
            }

            test("blames the tag scope when it leaves no entry") {
                val tagged = fileWith(notAffected)

                val reason =
                    cycloneDxVexEmptyReason(tagged, collectCycloneDxVexVulnerabilities(tagged, scope("1.0.0", "cli")))

                reason shouldBe CycloneDxVexEmptyReason.NO_ENTRY_IN_TAG_SCOPE
            }

            test("blames the release when no entry applies to it") {
                val later = fileWith(laterOnly)

                val reason = cycloneDxVexEmptyReason(later, collectCycloneDxVexVulnerabilities(later, scope("1.0.0")))

                reason shouldBe CycloneDxVexEmptyReason.NO_ENTRY_ON_RELEASE
            }
        }

        context("carryOverCycloneDxVexTimes") {
            val issued = Instant.parse("2026-04-25T00:00:00Z")
            val vulnerability =
                CycloneDxVexVulnerability(
                    id = cve("CVE-2026-3333"),
                    aliases = emptyList(),
                    description = null,
                    packages = listOf(libPurl),
                    status = VexStatus.UnderInvestigation(),
                    firstIssued = VexStatementTime.Issued,
                )
            val baseline =
                CycloneDxVexBaseline(
                    CycloneDxVexFormatVersion.VERSION_1_7,
                    CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79"),
                    VexDocumentVersion.FIRST,
                    listOf(vulnerability.copy(firstIssued = VexStatementTime.Carried(issued))),
                )

            test("an undated vulnerability equal to a baseline one keeps the time it was first issued") {
                val carried = carryOverCycloneDxVexTimes(listOf(vulnerability), baseline)

                carried.single().firstIssued shouldBe VexStatementTime.Carried(issued)
            }

            test("a changed vulnerability and a dated one are issued as they are") {
                val changed = vulnerability.copy(status = VexStatus.Fixed)
                val dated = vulnerability.copy(firstIssued = VexStatementTime.Stated(LocalDate.of(2026, 4, 18)))

                val carried = carryOverCycloneDxVexTimes(listOf(changed, dated), baseline)

                carried.map { it.firstIssued } shouldContainExactly
                    listOf(VexStatementTime.Issued, VexStatementTime.Stated(LocalDate.of(2026, 4, 18)))
            }

            test("without a baseline nothing is carried") {
                carryOverCycloneDxVexTimes(listOf(vulnerability), null) shouldContainExactly listOf(vulnerability)
            }
        }
    })
