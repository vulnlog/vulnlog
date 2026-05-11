// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.Impact
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.model.report.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private val defaultSchema = SchemaVersion(1, 0)
private val defaultProject = Project("org", "project", "author")
private val releaseV1 = Release("v1.0")
private val releaseV2 = Release("v2.0")
private val releaseV3 = Release("v3.0")
private val cve1 = VulnId.Cve("CVE-2024-0001")
private val cve2 = VulnId.Cve("CVE-2024-0002")
private val ghsa1 = VulnId.Ghsa("GHSA-1234-5678-abcd")

private fun vulnlogFile(
    project: Project = _root_ide_package_.dev.vulnlog.lib.core.defaultProject,
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = _root_ide_package_.dev.vulnlog.lib.core.defaultSchema,
    project = project,
    releases = emptyList(),
    vulnerabilities = vulnerabilities,
)

private fun vulnerability(
    id: VulnId = _root_ide_package_.dev.vulnlog.lib.core.cve1,
    aliases: List<VulnId> = emptyList(),
    releases: List<Release> = listOf(_root_ide_package_.dev.vulnlog.lib.core.releaseV1),
    analysis: String? = "not affected",
    verdict: Verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
    resolution: Resolution? = null,
    description: String? = null,
) = VulnerabilityEntry(
    id = id,
    aliases = aliases,
    releases = releases,
    packages = listOf(Purl.Maven("pkg:maven/com.example/lib@1.0")),
    reports = listOf(ReportEntry(reporter = ReporterType.TRIVY)),
    analysis = analysis,
    verdict = verdict,
    resolution = resolution,
    description = description,
)

private fun reportingEntry(
    primaryId: VulnId = _root_ide_package_.dev.vulnlog.lib.core.cve1,
    state: WorkState = WorkState.OPEN,
    ids: Set<VulnId> = setOf(primaryId),
    impact: Impact = Impact.NotAffected("vulnerable code not in execute path"),
    analysis: String? = "not affected",
    reportFor: Set<Release> = setOf(_root_ide_package_.dev.vulnlog.lib.core.releaseV1),
    fixedIn: Set<Release> = emptySet(),
    shortDescription: String? = null,
) = ReportingEntry(
    primaryId = primaryId,
    state = state,
    ids = ids,
    shortDescription = shortDescription,
    impact = impact,
    analysis = analysis,
    reportFor = reportFor,
    fixedIn = fixedIn,
)

class ReportingTest :
    FunSpec({

        context("collectReportingEntries") {

            test("maps primary id to separate field") {
                val file = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1, aliases = listOf(ghsa1))))

                val result = collectReportingEntries(file)

                result shouldHaveSize 1
                val entry = result.first()
                entry.primaryId shouldBe cve1
                entry.ids shouldBe setOf(ghsa1)
            }

            test("maps fixedIn as set from resolution") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().fixedIn shouldBe setOf(releaseV2)
            }

            test("fixedIn is empty set when no resolution") {
                val file = vulnlogFile(vulnerabilities = listOf(vulnerability()))

                val result = collectReportingEntries(file)

                result.first().fixedIn shouldBe emptySet()
            }

            test("applies filter by release") {
                val vuln1 = vulnerability(id = cve1, releases = listOf(releaseV1))
                val vuln2 = vulnerability(id = cve2, releases = listOf(releaseV2))
                val file = vulnlogFile(vulnerabilities = listOf(vuln1, vuln2))

                val result = collectReportingEntries(file, VulnlogFilter(releases = setOf(releaseV1)))

                result shouldHaveSize 1
                result.first().primaryId shouldBe cve1
            }

            test("derives under investigation state from UnderInvestigation verdict") {
                val vuln = vulnerability(verdict = Verdict.UnderInvestigation, analysis = null)
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.UNDER_INVESTIGATION
            }

            test("derives open state from Affected verdict without resolution") {
                val vuln = vulnerability(verdict = Verdict.Affected(Severity.HIGH))
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.OPEN
            }

            test("derives dismissed state from NotAffected verdict without resolution") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.DISMISSED
            }

            test("derives dismissed state from RiskAcceptable verdict without resolution") {
                val vuln = vulnerability(verdict = Verdict.RiskAcceptable(Severity.LOW))
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.DISMISSED
            }

            test("resolution overrides Affected verdict to resolved state") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.RESOLVED
            }

            test("NotAffected with resolution moves from dismissed to resolved and populates fixedIn") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.RESOLVED
                result.first().fixedIn shouldBe setOf(releaseV2)
            }

            test("UnderInvestigation verdict takes precedence over resolution") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.UnderInvestigation,
                        analysis = null,
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.UNDER_INVESTIGATION
            }

            test("resolution overrides RiskAcceptable verdict to resolved state") {
                val vuln =
                    vulnerability(
                        verdict = Verdict.RiskAcceptable(Severity.LOW),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file)

                result.first().state shouldBe WorkState.RESOLVED
            }

            test("Affected with resolution outside filter releases stays OPEN") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1),
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file, VulnlogFilter(releases = setOf(releaseV1)))

                result.first().state shouldBe WorkState.OPEN
            }

            test("Affected with resolution inside filter releases is RESOLVED") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1, releaseV2),
                        verdict = Verdict.Affected(Severity.HIGH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result =
                    collectReportingEntries(file, VulnlogFilter(releases = setOf(releaseV1, releaseV2)))

                result.first().state shouldBe WorkState.RESOLVED
            }

            test("NotAffected with resolution outside filter releases falls back to DISMISSED") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1),
                        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file, VulnlogFilter(releases = setOf(releaseV1)))

                result.first().state shouldBe WorkState.DISMISSED
            }

            test("RiskAcceptable with resolution outside filter releases falls back to DISMISSED") {
                val vuln =
                    vulnerability(
                        releases = listOf(releaseV1),
                        verdict = Verdict.RiskAcceptable(Severity.LOW),
                        resolution = Resolution(release = releaseV2),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))

                val result = collectReportingEntries(file, VulnlogFilter(releases = setOf(releaseV1)))

                result.first().state shouldBe WorkState.DISMISSED
            }
        }

        context("mergeReportingEntries") {

            test("merges entries with same id and same verdict") {
                val entry1 = reportingEntry(reportFor = setOf(releaseV1))
                val entry2 = reportingEntry(reportFor = setOf(releaseV2))

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 1
                result.first().reportFor shouldBe setOf(releaseV1, releaseV2)
            }

            test("unions alias ids when merging") {
                val entry1 = reportingEntry(ids = setOf(cve1))
                val entry2 = reportingEntry(ids = setOf(cve1, ghsa1))

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 1
                result.first().ids shouldBe setOf(cve1, ghsa1)
            }

            test("unions fixedIn releases when merging") {
                val entry1 = reportingEntry(fixedIn = setOf(releaseV1))
                val entry2 = reportingEntry(fixedIn = setOf(releaseV2))

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 1
                result.first().fixedIn shouldBe setOf(releaseV1, releaseV2)
            }

            test("picks first non-null short description") {
                val entry1 = reportingEntry(shortDescription = null)
                val entry2 = reportingEntry(shortDescription = "RCE in lib")

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 1
                result.first().shortDescription shouldBe "RCE in lib"
            }

            test("keeps entries separate when verdict differs") {
                val entry1 =
                    reportingEntry(
                        impact = Impact.Affected(Severity.HIGH),
                        state = WorkState.OPEN,
                        reportFor = setOf(releaseV1),
                    )
                val entry2 =
                    reportingEntry(
                        impact = Impact.NotAffected("not reachable"),
                        state = WorkState.OPEN,
                        reportFor = setOf(releaseV2),
                    )

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 2
            }

            test("keeps entries separate when analysis differs") {
                val entry1 = reportingEntry(analysis = "not reachable")
                val entry2 = reportingEntry(analysis = "mitigated by WAF")

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 2
            }

            test("keeps entries separate when state differs") {
                val entry1 = reportingEntry(state = WorkState.OPEN)
                val entry2 = reportingEntry(state = WorkState.RESOLVED)

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 2
            }

            test("does not merge entries with different primary ids") {
                val entry1 = reportingEntry(primaryId = cve1)
                val entry2 = reportingEntry(primaryId = cve2)

                val result = mergeReportingEntries(listOf(entry1, entry2))

                result shouldHaveSize 2
            }

            test("handles empty input") {
                val result = mergeReportingEntries(emptyList())

                result shouldHaveSize 0
            }

            test("single entry passes through unchanged") {
                val entry = reportingEntry()

                val result = mergeReportingEntries(listOf(entry))

                result shouldHaveSize 1
                result.first() shouldBe entry
            }

            test("merges three entries for same vulnerability") {
                val entry1 = reportingEntry(reportFor = setOf(releaseV1), fixedIn = setOf(releaseV1))
                val entry2 = reportingEntry(reportFor = setOf(releaseV2), fixedIn = setOf(releaseV2))
                val entry3 = reportingEntry(reportFor = setOf(releaseV3), fixedIn = setOf(releaseV3))

                val result = mergeReportingEntries(listOf(entry1, entry2, entry3))

                result shouldHaveSize 1
                result.first().reportFor shouldBe setOf(releaseV1, releaseV2, releaseV3)
                result.first().fixedIn shouldBe setOf(releaseV1, releaseV2, releaseV3)
            }
        }

        context("validateSharedProject") {

            test("returns project when all files share the same project") {
                val file1 = vulnlogFile()
                val file2 = vulnlogFile()

                val result = validateSharedProject(listOf(file1, file2))

                result.shouldNotBeNull()
                result shouldBe defaultProject
            }

            test("returns null when projects differ") {
                val file1 = vulnlogFile()
                val file2 = vulnlogFile(project = Project("other-org", "other", "other-author"))

                val result = validateSharedProject(listOf(file1, file2))

                result.shouldBeNull()
            }

            test("returns project for single file") {
                val result = validateSharedProject(listOf(vulnlogFile()))

                result.shouldNotBeNull()
                result shouldBe defaultProject
            }
        }
    })
