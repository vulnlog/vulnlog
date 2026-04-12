package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.Purl
import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReportEntry
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Resolution
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VexJustification
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.model.report.Impact
import dev.vulnlog.cli.model.report.ReportingEntry
import dev.vulnlog.cli.model.report.WorkState
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
    project: Project = defaultProject,
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = defaultSchema,
    project = project,
    releases = emptyList(),
    vulnerabilities = vulnerabilities,
)

private fun vulnerability(
    id: VulnId = cve1,
    aliases: List<VulnId> = emptyList(),
    releases: List<Release> = listOf(releaseV1),
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
    primaryId: VulnId = cve1,
    state: WorkState = WorkState.OPEN,
    ids: Set<VulnId> = setOf(primaryId),
    impact: Impact = Impact.NotAffected("vulnerable code not in execute path"),
    analysis: String? = "not affected",
    reportFor: Set<Release> = setOf(releaseV1),
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
