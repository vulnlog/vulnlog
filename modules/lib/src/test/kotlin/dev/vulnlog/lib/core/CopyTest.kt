// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import java.time.LocalDate

private val release1 = Release("1.0.0")
private val release2 = Release("2.0.0")
private val cve1 = VulnId.Cve("CVE-2026-1234")
private val cve2 = VulnId.Cve("CVE-2026-5678")
private val ghsa1 = VulnId.Ghsa("GHSA-1234-5678-abcd")

private fun vulnerability(
    id: VulnId = cve1,
    name: String? = null,
    aliases: List<VulnId> = emptyList(),
    releases: List<Release> = listOf(release2),
    description: String? = null,
    packages: List<Purl> = listOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
    reports: List<ReportEntry> = listOf(ReportEntry(reporter = ReporterType.TRIVY)),
    tags: List<Tag> = emptyList(),
    analysis: String? = null,
    analyzedAt: LocalDate? = null,
    verdict: Verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
) = VulnerabilityEntry(
    id = id,
    name = name,
    aliases = aliases,
    releases = releases,
    description = description,
    packages = packages,
    reports = reports,
    tags = tags,
    analysis = analysis,
    analyzedAt = analyzedAt,
    verdict = verdict,
)

private fun vulnlogFile(
    releases: List<ReleaseEntry> = listOf(ReleaseEntry(id = release1, publicationDate = LocalDate.of(2026, 1, 1))),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = SchemaVersion(1, 0),
    project = Project("Acme", "App", "Sec"),
    releases = releases,
    vulnerabilities = vulnerabilities,
)

class CopyTest :
    FunSpec({

        context("lastReleaseFavoringPublished") {

            test("returns last published release") {
                val releases =
                    listOf(
                        ReleaseEntry(id = Release("1.0.0"), publicationDate = LocalDate.of(2025, 1, 1)),
                        ReleaseEntry(id = Release("1.1.0"), publicationDate = LocalDate.of(2025, 6, 1)),
                        ReleaseEntry(id = Release("1.2.0")),
                    )

                lastReleaseFavoringPublished(releases) shouldBe Release("1.1.0")
            }

            test("falls back to last release when none is published") {
                val releases =
                    listOf(
                        ReleaseEntry(id = Release("1.0.0")),
                        ReleaseEntry(id = Release("1.1.0")),
                    )

                lastReleaseFavoringPublished(releases) shouldBe Release("1.1.0")
            }
        }

        context("findNonExistingVulnIds") {

            test("returns the requested ids that are not present") {
                val vulns = listOf(vulnerability(id = cve1), vulnerability(id = cve2))

                val missing = findNonExistingVulnIds(vulns, setOf(cve1, ghsa1))

                missing shouldBe setOf(ghsa1)
            }

            test("returns empty set when all requested ids exist") {
                val vulns = listOf(vulnerability(id = cve1))
                findNonExistingVulnIds(vulns, setOf(cve1)) shouldBe emptySet()
            }
        }

        context("formatCopiedMessage") {

            test("renders copied ids when non-empty") {
                formatCopiedMessage(Path.of("/tmp/x.vl.yaml"), listOf(cve1, cve2)) shouldBe
                    "Copied to /tmp/x.vl.yaml: CVE-2026-1234, CVE-2026-5678"
            }

            test("renders no-op message when empty") {
                formatCopiedMessage(Path.of("/tmp/x.vl.yaml"), emptyList()) shouldBe
                    "No new vulnerabilities to copy to /tmp/x.vl.yaml"
            }
        }

        context("formatVulnIdsNotInSourceMessage") {

            test("renders missing ids") {
                formatVulnIdsNotInSourceMessage(setOf(cve1, ghsa1)) shouldContain
                    "Vulnerability IDs not found in source file:"
                formatVulnIdsNotInSourceMessage(setOf(cve1)) shouldContain "CVE-2026-1234"
            }
        }

        context("serializeEntryYaml") {

            test("produces correctly indented YAML list item") {
                val dto =
                    VulnerabilityEntryDto(
                        id = "CVE-2026-1234",
                        releases = listOf("1.0.0"),
                        packages = listOf("pkg:npm/example-lib@2.3.0"),
                        reports = listOf(ReportEntryDto(reporter = "trivy")),
                        verdict = "not affected",
                        justification = "vulnerable code not in execute path",
                    )

                val yaml = serializeEntryYaml(dto, createYamlMapper())

                yaml shouldContain "  - id: "
                yaml shouldContain "    releases:"
                yaml shouldContain "    verdict: "
                yaml shouldNotContain "---"
            }

            test("omits null fields") {
                val dto =
                    VulnerabilityEntryDto(
                        id = "CVE-2026-1234",
                        releases = listOf("1.0.0"),
                        packages = listOf("pkg:npm/example-lib@2.3.0"),
                        reports = listOf(ReportEntryDto(reporter = "trivy")),
                    )

                val yaml = serializeEntryYaml(dto, createYamlMapper())

                yaml shouldNotContain "verdict:"
                yaml shouldNotContain "severity:"
                yaml shouldNotContain "justification:"
                yaml shouldNotContain "analysis:"
                yaml shouldNotContain "description:"
                yaml shouldNotContain "comment:"
                yaml shouldNotContain "resolution:"
                yaml shouldNotContain "source:"
                yaml shouldNotContain "suppress:"
            }

            test("omits empty lists") {
                val dto =
                    VulnerabilityEntryDto(
                        id = "CVE-2026-1234",
                        releases = listOf("1.0.0"),
                        packages = listOf("pkg:npm/example-lib@2.3.0"),
                        reports = listOf(ReportEntryDto(reporter = "trivy")),
                        aliases = emptyList(),
                        tags = emptyList(),
                    )

                val yaml = serializeEntryYaml(dto, createYamlMapper())

                yaml shouldNotContain "aliases:"
                yaml shouldNotContain "tags:"
                yaml shouldNotContain "vuln_ids:"
            }
        }

        context("insertEntryAfterVulnerabilitiesHeader") {

            test("inserts after header") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: "CVE-2026-0001"
                    """.trimMargin()
                val entryYaml = "  - id: \"CVE-2026-9999\"\n    releases:\n      - \"1.0.0\""

                val result = insertEntryAfterVulnerabilitiesHeader(fileContent, entryYaml)

                val lines = result.lines()
                val headerIndex = lines.indexOf("vulnerabilities:")
                lines[headerIndex + 2] shouldBe "  - id: \"CVE-2026-9999\""
            }

            test("handles empty vulnerabilities list") {
                val fileContent = "vulnerabilities: []"
                val entryYaml = "  - id: \"CVE-2026-9999\"\n    releases:\n      - \"1.0.0\""

                val result = insertEntryAfterVulnerabilitiesHeader(fileContent, entryYaml)

                result shouldContain "vulnerabilities:"
                result shouldNotContain "[]"
                result shouldContain "CVE-2026-9999"
            }
        }

        context("replaceEntryById") {

            test("replaces a middle entry and preserves blank separators") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-0001
                    |    releases: [ 1.0.0 ]
                    |
                    |  - id: CVE-2026-1234
                    |    releases: [ 1.0.0 ]
                    |    description: old
                    |
                    |  - id: CVE-2026-5678
                    |    releases: [ 1.0.0 ]
                    """.trimMargin()
                val newEntry = "  - id: CVE-2026-1234\n    releases: [ 2.0.0 ]\n    description: new"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                result shouldContain "description: new"
                result shouldNotContain "description: old"
                // separator blank lines kept on both sides
                result shouldContain "    releases: [ 1.0.0 ]\n\n  - id: CVE-2026-1234"
                result shouldContain "    description: new\n\n  - id: CVE-2026-5678"
                // the other entries are untouched
                result shouldContain "  - id: CVE-2026-0001"
                result shouldContain "  - id: CVE-2026-5678"
            }

            test("replaces the last entry in the section") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-0001
                    |    releases: [ 1.0.0 ]
                    |
                    |  - id: CVE-2026-1234
                    |    releases: [ 1.0.0 ]
                    |    description: old
                    """.trimMargin()
                val newEntry = "  - id: CVE-2026-1234\n    description: new"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                result shouldContain "description: new"
                result shouldNotContain "description: old"
                result shouldContain "  - id: CVE-2026-0001"
            }

            test("matches a quoted id") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: "CVE-2026-1234"
                    |    description: old
                    """.trimMargin()
                val newEntry = "  - id: \"CVE-2026-1234\"\n    description: new"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                result shouldContain "description: new"
                result shouldNotContain "description: old"
            }

            test("falls back to inserting when the id is not present") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-0001
                    |    releases: [ 1.0.0 ]
                    """.trimMargin()
                val newEntry = "  - id: CVE-2026-1234\n    releases: [ 1.0.0 ]"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                result shouldContain "CVE-2026-1234"
                result shouldContain "CVE-2026-0001"
            }

            test("stops at the next top-level key when the entry is the last one") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-1234
                    |    description: old
                    |
                    |something_else:
                    |  key: value
                    """.trimMargin()
                val newEntry = "  - id: CVE-2026-1234\n    description: new"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                result shouldContain "description: new"
                result shouldNotContain "description: old"
                result shouldContain "something_else:"
                result shouldContain "  key: value"
            }

            test("does not match an id substring inside a description") {
                val fileContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-0001
                    |    description: see CVE-2026-1234 for context
                    """.trimMargin()
                val newEntry = "  - id: CVE-2026-1234\n    description: new"

                val result = replaceEntryById(fileContent, cve1, newEntry)

                // id wasn't found as a real entry → falls back to inserting after the header
                result shouldContain "see CVE-2026-1234 for context"
                result shouldContain "  - id: CVE-2026-1234"
            }
        }

        context("copyVulnerabilities") {

            val sourceContent =
                """
                |vulnerabilities:
                """.trimMargin()
            val destinationContentEmpty =
                """
                |vulnerabilities:
                """.trimMargin()

            test("inserts an entry that does not exist in the destination") {
                val source =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(id = cve1, description = "from source")),
                    )
                val destination = vulnlogFile()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = destination,
                        destinationContent = destinationContentEmpty,
                        vulnIds = setOf(cve1),
                    )

                outcome.copied shouldContainExactly listOf(cve1)
                outcome.newContent shouldContain "CVE-2026-1234"
                outcome.newContent shouldContain "from source"
                // release rewritten to destination's latest published release (1.0.0)
                outcome.newContent shouldContain "1.0.0"
                outcome.newContent shouldNotContain "2.0.0"
            }

            test("merges with existing entry: existing scalars win, source fills nulls") {
                val source =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve1,
                                    description = "source description",
                                    analysis = "source analysis",
                                    name = "source name",
                                ),
                            ),
                    )
                val destination =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve1,
                                    description = "existing description", // existing wins
                                    analysis = null, // null in existing → falls back to source
                                    name = null, // null in existing → falls back to source
                                    releases = listOf(release1),
                                ),
                            ),
                    )
                val destinationContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-1234
                    |    releases: [ 1.0.0 ]
                    |    description: existing description
                    |    packages: [ "pkg:npm/example-lib@2.3.0" ]
                    |    reports:
                    |      - reporter: trivy
                    |    verdict: not affected
                    |    justification: vulnerable code not in execute path
                    """.trimMargin()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = destination,
                        destinationContent = destinationContent,
                        vulnIds = setOf(cve1),
                    )

                outcome.copied shouldContainExactly listOf(cve1)
                outcome.newContent shouldContain "existing description"
                outcome.newContent shouldNotContain "source description"
                outcome.newContent shouldContain "source analysis"
                outcome.newContent shouldContain "source name"
                // exactly one entry — replace, not duplicate
                "CVE-2026-1234".toRegex().findAll(outcome.newContent).count() shouldBe 1
            }

            test("merges lists by union (aliases, packages, tags)") {
                val source =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve1,
                                    aliases = listOf(ghsa1),
                                    packages = listOf(Purl.Npm("pkg:npm/source-only@1.0")),
                                ),
                            ),
                    )
                val destination =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve1,
                                    aliases = emptyList(),
                                    packages = listOf(Purl.Npm("pkg:npm/dest-only@1.0")),
                                    releases = listOf(release1),
                                ),
                            ),
                    )
                val destinationContent =
                    """
                    |vulnerabilities:
                    |
                    |  - id: CVE-2026-1234
                    |    releases: [ 1.0.0 ]
                    |    packages: [ "pkg:npm/dest-only@1.0" ]
                    |    reports:
                    |      - reporter: trivy
                    |    verdict: not affected
                    |    justification: vulnerable code not in execute path
                    """.trimMargin()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = destination,
                        destinationContent = destinationContent,
                        vulnIds = setOf(cve1),
                    )

                // packages: union (existing first, then unique additions)
                outcome.newContent shouldContain "pkg:npm/dest-only@1.0"
                outcome.newContent shouldContain "pkg:npm/source-only@1.0"
                // aliases: source's alias appears
                outcome.newContent shouldContain "GHSA-1234-5678-abcd"
            }

            test("rewrites releases to destination's latest published release") {
                val source =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(id = cve1, releases = listOf(release2))),
                    )
                val destination =
                    vulnlogFile(
                        releases =
                            listOf(
                                ReleaseEntry(id = release1, publicationDate = LocalDate.of(2025, 1, 1)),
                                ReleaseEntry(id = Release("1.5.0")),
                            ),
                    )

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = destination,
                        destinationContent = destinationContentEmpty,
                        vulnIds = setOf(cve1),
                    )

                outcome.newContent shouldContain "\"1.0.0\""
                outcome.newContent shouldNotContain "1.5.0"
                outcome.newContent shouldNotContain "2.0.0"
            }

            test("ignores ids in vulnIds that are not present in the source") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1)))

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = vulnlogFile(),
                        destinationContent = destinationContentEmpty,
                        vulnIds = setOf(cve2), // not in source
                    )

                outcome.copied shouldBe emptyList()
                outcome.newContent shouldNotContain "CVE-2026-5678"
            }
        }
    })
