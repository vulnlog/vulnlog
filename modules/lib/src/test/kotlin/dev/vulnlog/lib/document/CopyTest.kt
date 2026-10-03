// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private val release1 = Release("1.0.0")
private val lastRelease = Release("1.5.0")
private val cve1 = VulnId.Cve("CVE-2026-1234")
private val cve2 = VulnId.Cve("CVE-2026-5678")
private val ghsa1 = VulnId.Ghsa("GHSA-1234-5678-abcd")

private fun vulnerability(
    id: VulnId = cve1,
    name: String? = null,
    aliases: List<VulnId> = emptyList(),
    releases: List<Release> = listOf(release1),
    description: String? = null,
    packages: List<Purl> = listOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
    reports: List<ReportEntry> = listOf(ReportEntry(reporter = ReporterType.TRIVY)),
    analysis: String? = null,
    verdict: Verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
) = VulnerabilityEntry(
    id = id,
    name = name,
    aliases = aliases,
    releases = releases,
    description = description,
    packages = packages,
    reports = reports,
    analysis = analysis,
    verdict = verdict,
)

private fun vulnlogFile(
    releases: List<Release> = listOf(release1),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = SchemaVersion.V1,
    project = Project("Acme", "App", "Sec"),
    releases = releases.map { ReleaseEntry(id = it, publicationDate = LocalDate.of(2026, 1, 1)) },
    vulnerabilities = vulnerabilities,
)

/** Rendered with `YamlWriter.write`, so the document starts with the `# $schema:` header. */
private fun render(file: VulnlogFile): String = YamlWriter.write(file)

/** Reads both files from YAML, as the drivers do. */
private fun copy(
    source: VulnlogFile,
    destination: VulnlogFile,
    vulnIds: Set<VulnId>,
): CopyOutcome =
    copyVulnerabilities(validated(render(source)).vulnlogProjectFile, validated(render(destination)), vulnIds)

private fun entriesOf(content: String): List<VulnerabilityEntry> =
    mapToDomain(parsed(content).validatedDto)
        .shouldBeInstanceOf<DomainMappingResult.Mapped>()
        .vulnlogProjectFile
        .vulnerabilities

class CopyTest :
    FunSpec({

        test("findNonExistingVulnIds returns the requested ids the entries lack") {
            val entries = listOf(vulnerability(id = cve1), vulnerability(id = cve2))
            val requests = listOf(setOf(cve1, ghsa1), setOf(cve1, cve2))

            val missing = requests.map { findNonExistingVulnIds(entries, it) }

            missing shouldBe listOf(setOf(ghsa1), emptySet())
        }

        context("copyVulnerabilities") {

            test("inserts a missing entry at the top, pointing at the destination's last release") {
                val source =
                    vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve2, description = "from source")))
                val destination =
                    vulnlogFile(
                        releases = listOf(release1, lastRelease),
                        vulnerabilities = listOf(vulnerability(id = cve1)),
                    )

                val outcome = copy(source, destination, setOf(cve2))

                val entries = entriesOf(outcome.newContent)
                outcome.copied shouldBe listOf(cve2)
                entries.map { it.id } shouldBe listOf(cve2, cve1)
                entries.first().releases shouldBe listOf(lastRelease)
                entries.first().description shouldBe "from source"
            }

            test("copies every requested entry in one pass") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1), vulnerability(id = cve2)))

                val outcome = copy(source, vulnlogFile(), setOf(cve1, cve2))

                outcome.copied shouldBe listOf(cve1, cve2)
                entriesOf(outcome.newContent).map { it.id } shouldContainExactlyInAnyOrder listOf(cve1, cve2)
            }

            test("keeps an existing entry's values, fills the ones it lacks and points it at the last release") {
                val source =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    description = "source description",
                                    analysis = "source analysis",
                                    verdict = Verdict.Affected(Severity.HIGH),
                                ),
                            ),
                    )
                val destination =
                    vulnlogFile(
                        releases = listOf(release1, lastRelease),
                        vulnerabilities = listOf(vulnerability(description = "existing description")),
                    )

                val outcome = copy(source, destination, setOf(cve1))

                val merged = entriesOf(outcome.newContent).single()
                merged.description shouldBe "existing description"
                merged.verdict shouldBe Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH)
                merged.analysis shouldBe "source analysis"
                merged.releases shouldBe listOf(lastRelease)
            }

            test("keeps the name of a copied entry and of the entry it merges into") {
                val source =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(id = cve1, name = "Log4Shell"),
                                vulnerability(id = cve2, name = "other"),
                            ),
                    )
                val destination =
                    vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve2, name = "Spring4Shell")))

                val outcome = copy(source, destination, setOf(cve1, cve2))

                entriesOf(outcome.newContent).associate { it.id to it.name } shouldBe
                    mapOf(cve1 to "Log4Shell", cve2 to "Spring4Shell")
            }

            test("unions the lists of an existing entry, its own items first") {
                val shared = Purl.Npm("pkg:npm/shared@1.0")
                val sourceOnly = Purl.Npm("pkg:npm/source-only@1.0")
                val destinationOnly = Purl.Npm("pkg:npm/destination-only@1.0")
                val source =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(vulnerability(aliases = listOf(ghsa1), packages = listOf(sourceOnly, shared))),
                    )
                val destination =
                    vulnlogFile(vulnerabilities = listOf(vulnerability(packages = listOf(destinationOnly, shared))))

                val outcome = copy(source, destination, setOf(cve1))

                val merged = entriesOf(outcome.newContent).single()
                merged.aliases shouldBe listOf(ghsa1)
                merged.packages shouldBe listOf(destinationOnly, shared, sourceOnly)
            }

            test("merges reports by reporter: an existing report keeps its values and gains the source's") {
                val existingReport =
                    ReportEntry(
                        reporter = ReporterType.TRIVY,
                        at = LocalDate.of(2026, 1, 10),
                        vulnIds = setOf(VulnId.Cve("CVE-2026-1111")),
                    )
                val sourceReports =
                    listOf(
                        ReportEntry(
                            reporter = ReporterType.TRIVY,
                            at = LocalDate.of(2026, 2, 20),
                            source = "nightly scan",
                            vulnIds = setOf(VulnId.Cve("CVE-2026-2222")),
                        ),
                        ReportEntry(reporter = ReporterType.SNYK),
                    )
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(reports = sourceReports)))
                val destination = vulnlogFile(vulnerabilities = listOf(vulnerability(reports = listOf(existingReport))))

                val outcome = copy(source, destination, setOf(cve1))

                entriesOf(outcome.newContent).single().reports shouldBe
                    listOf(
                        existingReport.copy(
                            source = "nightly scan",
                            vulnIds = setOf(VulnId.Cve("CVE-2026-1111"), VulnId.Cve("CVE-2026-2222")),
                        ),
                        ReportEntry(reporter = ReporterType.SNYK),
                    )
            }

            test("ignores requested ids the source lacks") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1)))

                val outcome = copy(source, vulnlogFile(), setOf(cve2))

                outcome.copied.shouldBeEmpty()
                entriesOf(outcome.newContent).shouldBeEmpty()
            }

            test("rewrites a destination in any layout canonically, so fmt changes nothing") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve2)))
                val column0Destination =
                    """
                    |schemaVersion: "1"
                    |project:
                    |  organization: Acme
                    |  name: App
                    |  author: Sec
                    |releases:
                    |- id: 1.0.0
                    |  published_at: 2026-01-01
                    |vulnerabilities:
                    |- id: CVE-2026-1234
                    |  releases: [1.0.0]
                    |  packages: ["pkg:npm/example-lib@2.3.0"]
                    |  reports:
                    |  - reporter: trivy
                    |  verdict: not affected
                    |  justification: vulnerable code not in execute path
                    """.trimMargin() + "\n"

                val outcome = copyVulnerabilities(source, validated(column0Destination), setOf(cve2))

                entriesOf(outcome.newContent).map { it.id } shouldBe listOf(cve2, cve1)
                formatYaml(parsed(outcome.newContent)) shouldBe outcome.newContent
            }

            test("keeps the schema header only where the destination has one") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability()))
                val withHeader = render(vulnlogFile())
                val withoutHeader = withHeader.substringAfter('\n')

                val firstLines =
                    listOf(withHeader, withoutHeader).map { destination ->
                        copyVulnerabilities(source, validated(destination), setOf(cve1)).newContent.lines().first()
                    }

                firstLines shouldBe listOf("# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json", "---")
            }
        }
    })
