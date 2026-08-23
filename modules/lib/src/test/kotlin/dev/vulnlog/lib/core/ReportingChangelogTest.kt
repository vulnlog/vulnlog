// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.core.reporting.collectChangelogReleases
import dev.vulnlog.lib.core.reporting.declaredReleases
import dev.vulnlog.lib.core.reporting.selectFixedVulnerabilities
import dev.vulnlog.lib.core.reporting.summarize
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.reporting.ChangelogSummary
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ReportingChangelogEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val first = cve("CVE-2024-0001")
private val second = cve("CVE-2024-0002")

private val threeReleases =
    listOf(
        releaseEntry("1.0.0", publishedAt = LocalDate.parse("2024-01-01")),
        releaseEntry("1.1.0", publishedAt = LocalDate.parse("2024-02-01")),
        releaseEntry("1.2.0"),
    )

/** An affected vulnerability reported for [reportedFor] whose fix shipped in [fixedIn]. */
private fun fixedVulnerability(
    id: VulnId = first,
    fixedIn: String = "1.1.0",
    severity: Severity = Severity.HIGH,
    aliases: List<VulnId> = emptyList(),
    reportedFor: List<String> = listOf("1.0.0"),
) = vulnerability(
    id,
    releases = reportedFor.map(::release),
    aliases = aliases,
    verdict = Verdict.Affected(severity),
    resolution = resolution(fixedIn),
)

private fun notAffectedButResolved() =
    vulnerability(
        first,
        releases = listOf(release("1.0.0")),
        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT),
        resolution = resolution("1.1.0"),
    )

private fun fileOf(
    vulnerabilities: List<VulnerabilityEntry>,
    releases: List<ReleaseEntry> = threeReleases,
) = vulnlogFile(releases = releases, vulnerabilities = vulnerabilities)

/** The fixes selected from one file declaring [threeReleases]. */
private fun selectedFixesOf(vulnerabilities: List<VulnerabilityEntry>) =
    selectFixedVulnerabilities(listOf(fileOf(vulnerabilities)), threeReleases.map { it.id })

private fun changelogOf(vulnerabilities: List<VulnerabilityEntry>) =
    collectChangelogReleases(listOf(fileOf(vulnerabilities)))

private fun affectedEntry(
    id: VulnId = first,
    severity: Severity = Severity.HIGH,
) = ReportingChangelogEntry(primaryId = id, impact = Impact.Affected(severity))

class ReportingChangelogTest :
    FunSpec({

        context("declared releases") {

            test("keeps the order the file declares them in") {
                val files = listOf(fileOf(emptyList()))

                val declared = declaredReleases(files)

                declared.map { it.id } shouldContainExactly
                    listOf(release("1.0.0"), release("1.1.0"), release("1.2.0"))
            }

            test("declares a release shared by two files once") {
                val files = listOf(fileOf(emptyList()), fileOf(emptyList()))

                val declared = declaredReleases(files)

                declared shouldHaveSize 3
            }
        }

        context("fixed vulnerabilities") {

            test("selects a vulnerability that records a resolution") {
                val resolved = fixedVulnerability()

                val fixed = selectedFixesOf(listOf(resolved))

                fixed.single().vulnerability.id shouldBe first
            }

            test("skips a vulnerability that records no resolution") {
                val open =
                    vulnerability(first, releases = listOf(release("1.0.0")), verdict = Verdict.Affected(Severity.HIGH))

                val fixed = selectedFixesOf(listOf(open))

                fixed.shouldBeEmpty()
            }

            test("selects a vulnerability reported for an earlier release and for the release that fixed it") {
                val carriedOver =
                    fixedVulnerability(fixedIn = "1.2.0", reportedFor = listOf("1.1.0", "1.2.0"))

                val fixed = selectedFixesOf(listOf(carriedOver))

                fixed.single().vulnerability.id shouldBe first
            }

            test("skips a vulnerability reported only for the release that fixed it") {
                val neverShipped = fixedVulnerability(fixedIn = "1.2.0", reportedFor = listOf("1.2.0"))

                val fixed = selectedFixesOf(listOf(neverShipped))

                fixed.shouldBeEmpty()
            }

            test("skips a vulnerability reported only for releases after the one that fixed it") {
                val notYetShipped = fixedVulnerability(fixedIn = "1.1.0", reportedFor = listOf("1.2.0"))

                val fixed = selectedFixesOf(listOf(notYetShipped))

                fixed.shouldBeEmpty()
            }

            test("selects a vulnerability judged not affected that still records a resolution") {
                val notAffected = notAffectedButResolved()

                val fixed = selectedFixesOf(listOf(notAffected))

                fixed.single().vulnerability.id shouldBe first
            }
        }

        context("summary") {

            test("counts how many vulnerabilities were fixed") {
                val entries = listOf(affectedEntry(), affectedEntry(id = second, severity = Severity.LOW))

                val summary = summarize(entries)

                summary.total shouldBe 2
            }

            test("counts per severity, most severe first") {
                val entries =
                    listOf(
                        affectedEntry(severity = Severity.LOW),
                        affectedEntry(id = second, severity = Severity.CRITICAL),
                    )

                val summary = summarize(entries)

                summary.bySeverity.toList() shouldContainExactly listOf(Severity.CRITICAL to 1, Severity.LOW to 1)
            }

            test("counts an entry without a severity in the total only") {
                val entries = listOf(ReportingChangelogEntry(primaryId = first, impact = Impact.NotAffected("reason")))

                val summary = summarize(entries)

                summary shouldBe ChangelogSummary(total = 1, bySeverity = emptyMap())
            }
        }

        context("grouping") {

            test("groups a vulnerability under the release that shipped its fix") {
                val vulnerabilities = listOf(fixedVulnerability(fixedIn = "1.1.0"))

                val releases = changelogOf(vulnerabilities)

                releases.single().fixedIn shouldBe release("1.1.0")
            }

            test("carries the publication date of the release") {
                val vulnerabilities = listOf(fixedVulnerability(fixedIn = "1.1.0"))

                val releases = changelogOf(vulnerabilities)

                releases.single().publishedAt shouldBe LocalDate.parse("2024-02-01")
            }

            test("leaves the publication date absent for a release that is not published yet") {
                val vulnerabilities = listOf(fixedVulnerability(fixedIn = "1.2.0"))

                val releases = changelogOf(vulnerabilities)

                releases.single().publishedAt.shouldBeNull()
            }

            test("merges the same release across two files into one section") {
                val files =
                    listOf(
                        fileOf(listOf(fixedVulnerability())),
                        fileOf(listOf(fixedVulnerability(id = second))),
                    )

                val releases = collectChangelogReleases(files)

                releases.single().entries shouldHaveSize 2
            }

            test("merges one vulnerability recorded in two files into one entry, keeping every alias") {
                val files =
                    listOf(
                        fileOf(listOf(fixedVulnerability(aliases = listOf(ghsa("GHSA-aaaa-bbbb-cccc"))))),
                        fileOf(listOf(fixedVulnerability(aliases = listOf(cve("CVE-2024-9999"))))),
                    )

                val releases = collectChangelogReleases(files)

                val merged = releases.single().entries.single()
                merged.aliases shouldBe setOf(ghsa("GHSA-aaaa-bbbb-cccc"), cve("CVE-2024-9999"))
            }

            test("summarizes what the release fixed") {
                val vulnerabilities = listOf(fixedVulnerability(severity = Severity.CRITICAL))

                val releases = changelogOf(vulnerabilities)

                releases.single().summary shouldBe ChangelogSummary(1, mapOf(Severity.CRITICAL to 1))
            }

            test("returns nothing when no vulnerability records a resolution") {
                val vulnerabilities = listOf(vulnerability(first, releases = listOf(release("1.0.0"))))

                val releases = changelogOf(vulnerabilities)

                releases.shouldBeEmpty()
            }
        }

        context("ordering") {

            test("returns the newest release first") {
                val vulnerabilities =
                    listOf(
                        fixedVulnerability(fixedIn = "1.1.0"),
                        fixedVulnerability(id = second, fixedIn = "1.2.0"),
                    )

                val releases = changelogOf(vulnerabilities)

                releases.map { it.fixedIn } shouldContainExactly listOf(release("1.2.0"), release("1.1.0"))
            }

            test("orders the entries of a release by severity, most severe first") {
                val vulnerabilities =
                    listOf(
                        fixedVulnerability(severity = Severity.LOW),
                        fixedVulnerability(id = second, severity = Severity.CRITICAL),
                    )

                val releases = changelogOf(vulnerabilities)

                releases.single().entries.map { it.primaryId } shouldContainExactly listOf(second, first)
            }

            test("orders entries of equal severity by identifier") {
                val vulnerabilities = listOf(fixedVulnerability(id = second), fixedVulnerability(id = first))

                val releases = changelogOf(vulnerabilities)

                releases.single().entries.map { it.primaryId } shouldContainExactly listOf(first, second)
            }
        }
    })
