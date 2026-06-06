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
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.Path
import java.time.LocalDate

private fun vulnlogFile(
    releases: List<ReleaseEntry> = listOf(ReleaseEntry(Release("1.0.0"), publicationDate = LocalDate.of(2026, 1, 15))),
    tags: List<TagEntry> = emptyList(),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
): VulnlogFile =
    VulnlogFile(
        schemaVersion = SchemaVersion(1, 0),
        project = Project("acme", "widget", "alice"),
        tags = tags,
        releases = releases,
        vulnerabilities = vulnerabilities,
    )

private fun renderContent(file: VulnlogFile): String = YamlWriter.write(file, createYamlMapper())

/**
 * Builds a Vulnlog YAML content string with list items indented by 2 spaces (matching real
 * Vulnlog files). [YamlWriter] currently emits zero-indent list items, which doesn't match the
 * regex used by `replaceEntryById`. Real CLI flows read 2-space-indented files from disk, so for
 * upsert tests we hand-craft content here. [entriesYaml] is appended verbatim (no re-indent), so
 * callers control the entry block indentation directly.
 */
private fun yamlWithEntries(entriesYaml: String): String {
    val header =
        """
        |---
        |schemaVersion: "1"
        |
        |project:
        |  organization: "acme"
        |  name: "widget"
        |  author: "alice"
        |
        |releases:
        |  - id: "1.0.0"
        |    published_at: "2026-01-15"
        |
        |vulnerabilities:
        |
        """.trimMargin()
    return header + entriesYaml + "\n"
}

private val DEFAULT_OPTIONS =
    AddVulnerabilityOptions(
        vulnId = VulnId.Cve("CVE-2026-1234"),
        releases = emptySet(),
        packages = emptySet(),
        tags = emptySet(),
        reporter = null,
    )

class AddTest :
    FunSpec({

        test("createVulnerabilityEntry with only a vuln-id emits empty list fields and no verdict") {
            val yaml = createVulnerabilityEntry(DEFAULT_OPTIONS)

            yaml shouldStartWith "  - id: CVE-2026-1234"
            yaml shouldContain "releases: []"
            yaml shouldContain "packages: []"
            yaml shouldContain "reports: []"
            yaml shouldNotContain "verdict"
            yaml shouldNotContain "tags"
        }

        test("createVulnerabilityEntry serializes releases, packages, tags and reporter with today's date") {
            val yaml =
                createVulnerabilityEntry(
                    AddVulnerabilityOptions(
                        vulnId = VulnId.Cve("CVE-2026-5678"),
                        releases = setOf(Release("1.0.0")),
                        packages = setOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
                        tags = setOf(Tag("frontend")),
                        reporter = ReporterType.TRIVY,
                    ),
                )

            yaml shouldContain "CVE-2026-5678"
            yaml shouldContain "releases: [1.0.0]"
            yaml shouldContain "pkg:npm/example-lib@2.3.0"
            yaml shouldContain "frontend"
            yaml shouldContain "reporter: trivy"
            yaml shouldContain "at: ${LocalDate.now()}"
        }

        context("addVulnerabilityToFile") {

            test("inserts the entry and falls back to the latest published release") {
                val file = vulnlogFile()
                val outcome = addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS)

                outcome.vulnId shouldBe VulnId.Cve("CVE-2026-1234")
                outcome.newContent shouldContain "CVE-2026-1234"
                outcome.newContent shouldContain "releases: [1.0.0]"
            }

            test("uses an explicitly supplied release") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                ReleaseEntry(Release("1.0.0"), publicationDate = LocalDate.of(2026, 1, 15)),
                                ReleaseEntry(Release("2.0.0"), publicationDate = LocalDate.of(2026, 3, 1)),
                            ),
                    )
                val outcome =
                    addVulnerabilityToFile(
                        file,
                        renderContent(file),
                        DEFAULT_OPTIONS.copy(releases = setOf(Release("1.0.0"))),
                    )

                outcome.newContent shouldContain "releases: [1.0.0]"
            }

            test("inserts the entry and falls back to the empty release when non exist in the Vulnlog file") {
                val file = vulnlogFile(releases = emptyList())
                val outcome = addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS)

                outcome.vulnId shouldBe VulnId.Cve("CVE-2026-1234")
                outcome.newContent shouldContain "CVE-2026-1234"
                outcome.newContent shouldNotContain "1.0.0"
            }

            test("attaches a tag that is defined in the file") {
                val file = vulnlogFile(tags = listOf(TagEntry(Tag("frontend"))))
                val outcome =
                    addVulnerabilityToFile(
                        file,
                        renderContent(file),
                        DEFAULT_OPTIONS.copy(tags = setOf(Tag("frontend"))),
                    )

                outcome.updated shouldBe false
                outcome.newContent shouldContain "tags: [frontend]"
            }

            test("inserts the entry in the canonical fmt style, so re-formatting is a no-op") {
                val file = vulnlogFile()
                val mapper = createYamlMapper()
                val outcome = addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS, mapper)

                formatYaml(VulnlogFileRaw(outcome.newContent), mapper).content shouldBe outcome.newContent
            }

            test("update preserves a literal block scalar from the destination file") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        description = "First line.\nSecond line.",
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = emptyList(),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file = vulnlogFile(vulnerabilities = listOf(existing))
                val content =
                    yamlWithEntries(
                        """
                        |  - id: "CVE-2026-1234"
                        |    description: |-
                        |      First line.
                        |      Second line.
                        |    releases:
                        |      - "1.0.0"
                        |    packages: []
                        |    reports: []
                        """.trimMargin(),
                    )

                val outcome =
                    addVulnerabilityToFile(
                        file,
                        content,
                        DEFAULT_OPTIONS.copy(packages = setOf(Purl.Npm("pkg:npm/lib@1.0.0"))),
                    )

                outcome.updated shouldBe true
                outcome.newContent shouldContain "description: |-"
                outcome.newContent shouldNotContain "description: >"
            }

            test("updates an existing entry in place, preserving unspecified fields") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        name = "Existing Name",
                        description = "Existing description.",
                        releases = listOf(Release("1.0.0")),
                        packages = listOf(Purl.Npm("pkg:npm/old-lib@1.0.0")),
                        reports = emptyList(),
                        tags = listOf(Tag("frontend")),
                        verdict = Verdict.Affected(Severity.HIGH),
                        comment = "Existing comment.",
                    )
                val file =
                    vulnlogFile(
                        tags = listOf(TagEntry(Tag("frontend"))),
                        vulnerabilities = listOf(existing),
                    )
                val content =
                    yamlWithEntries(
                        """
                        |  - id: "CVE-2026-1234"
                        |    name: "Existing Name"
                        |    description: "Existing description."
                        |    releases:
                        |      - "1.0.0"
                        |    packages:
                        |      - "pkg:npm/old-lib@1.0.0"
                        |    reports: []
                        |    tags:
                        |      - "frontend"
                        |    verdict: affected
                        |    severity: high
                        |    comment: "Existing comment."
                        """.trimMargin(),
                    )

                val outcome =
                    addVulnerabilityToFile(
                        file,
                        content,
                        DEFAULT_OPTIONS.copy(packages = setOf(Purl.Npm("pkg:npm/new-lib@2.0.0"))),
                    )

                outcome.updated shouldBe true
                outcome.newContent shouldContain "pkg:npm/new-lib@2.0.0"
                outcome.newContent shouldNotContain "pkg:npm/old-lib@1.0.0"
                outcome.newContent shouldContain "Existing Name"
                outcome.newContent shouldContain "Existing description."
                outcome.newContent shouldContain "frontend"
                outcome.newContent shouldContain "affected"
                outcome.newContent shouldContain "high"
                outcome.newContent shouldContain "Existing comment."
            }

            test("update with no --release keeps existing releases (no fallback to latest)") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = emptyList(),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                ReleaseEntry(Release("1.0.0"), publicationDate = LocalDate.of(2026, 1, 15)),
                                ReleaseEntry(Release("2.0.0"), publicationDate = LocalDate.of(2026, 3, 1)),
                            ),
                        vulnerabilities = listOf(existing),
                    )
                val content =
                    """
                    |---
                    |schemaVersion: "1"
                    |
                    |project:
                    |  organization: "acme"
                    |  name: "widget"
                    |  author: "alice"
                    |
                    |releases:
                    |  - id: "1.0.0"
                    |    published_at: "2026-01-15"
                    |  - id: "2.0.0"
                    |    published_at: "2026-03-01"
                    |
                    |vulnerabilities:
                    |
                    |  - id: "CVE-2026-1234"
                    |    releases:
                    |      - "1.0.0"
                    |    packages: []
                    |    reports: []
                    """.trimMargin()

                val outcome = addVulnerabilityToFile(file, content, DEFAULT_OPTIONS)

                outcome.updated shouldBe true
                val entryStart = outcome.newContent.indexOf("- id: CVE-2026-1234")
                val entryBody = outcome.newContent.substring(entryStart)
                entryBody shouldContain "releases: [1.0.0]"
                entryBody shouldNotContain "2.0.0"
            }

            test("update with --reporter appends a new ReportEntry when reporter is new") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = listOf(ReportEntry(reporter = ReporterType.TRIVY, at = LocalDate.of(2026, 1, 10))),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file = vulnlogFile(vulnerabilities = listOf(existing))
                val content =
                    yamlWithEntries(
                        """
                        |  - id: "CVE-2026-1234"
                        |    releases:
                        |      - "1.0.0"
                        |    packages: []
                        |    reports:
                        |      - reporter: trivy
                        |        at: "2026-01-10"
                        """.trimMargin(),
                    )

                val outcome =
                    addVulnerabilityToFile(
                        file,
                        content,
                        DEFAULT_OPTIONS.copy(reporter = ReporterType.SNYK),
                    )

                outcome.updated shouldBe true
                outcome.newContent shouldContain "trivy"
                outcome.newContent shouldContain "2026-01-10"
                outcome.newContent shouldContain "snyk"
                outcome.newContent shouldContain "${LocalDate.now()}"
            }

            test("update with --reporter updates the date when the reporter already has a report") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = listOf(ReportEntry(reporter = ReporterType.TRIVY, at = LocalDate.of(2026, 1, 10))),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file = vulnlogFile(vulnerabilities = listOf(existing))
                val content =
                    yamlWithEntries(
                        """
                        |  - id: "CVE-2026-1234"
                        |    releases:
                        |      - "1.0.0"
                        |    packages: []
                        |    reports:
                        |      - reporter: trivy
                        |        at: "2026-01-10"
                        """.trimMargin(),
                    )

                val outcome =
                    addVulnerabilityToFile(
                        file,
                        content,
                        DEFAULT_OPTIONS.copy(reporter = ReporterType.TRIVY),
                    )

                outcome.updated shouldBe true
                outcome.newContent shouldContain "trivy"
                outcome.newContent shouldContain "${LocalDate.now()}"
                outcome.newContent shouldNotContain "2026-01-10"
            }

            test("update preserves the position of the entry in the file") {
                val first =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-0001"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = emptyList(),
                        verdict = Verdict.UnderInvestigation,
                    )
                val second =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = emptyList(),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file = vulnlogFile(vulnerabilities = listOf(first, second))
                val content =
                    yamlWithEntries(
                        """
                        |  - id: "CVE-2026-0001"
                        |    releases:
                        |      - "1.0.0"
                        |    packages: []
                        |    reports: []
                        |
                        |  - id: "CVE-2026-1234"
                        |    releases:
                        |      - "1.0.0"
                        |    packages: []
                        |    reports: []
                        """.trimMargin(),
                    )

                val outcome =
                    addVulnerabilityToFile(
                        file,
                        content,
                        DEFAULT_OPTIONS.copy(packages = setOf(Purl.Npm("pkg:npm/lib@1.0.0"))),
                    )

                val firstIndex = outcome.newContent.indexOf("CVE-2026-0001")
                val secondIndex = outcome.newContent.indexOf("CVE-2026-1234")
                firstIndex shouldBeLessThan secondIndex
                // Each id should appear exactly once (not duplicated by accidental insert)
                outcome.newContent.split("CVE-2026-1234").size - 1 shouldBe 1
                outcome.newContent.split("CVE-2026-0001").size - 1 shouldBe 1
            }

            test("throws when --release is not defined in the file") {
                val file = vulnlogFile()

                shouldThrow<IllegalArgumentException> {
                    addVulnerabilityToFile(
                        file,
                        renderContent(file),
                        DEFAULT_OPTIONS.copy(releases = setOf(Release("9.9.9"))),
                    )
                }.message shouldContain "not defined"
            }

            test("throws when --tag is not defined in the file") {
                val file = vulnlogFile(tags = listOf(TagEntry(Tag("frontend"))))

                shouldThrow<IllegalArgumentException> {
                    addVulnerabilityToFile(
                        file,
                        renderContent(file),
                        DEFAULT_OPTIONS.copy(tags = setOf(Tag("unknown"))),
                    )
                }.message shouldContain "not defined"
            }
        }

        context("formatAddOutcomeMessage") {

            test("renders an added message on insert") {
                val outcome = AddOutcome(newContent = "", vulnId = VulnId.Cve("CVE-2026-1234"), updated = false)

                formatAddOutcomeMessage(Path.of("/tmp/x.vl.yaml"), outcome) shouldBe
                    "Added to /tmp/x.vl.yaml: CVE-2026-1234"
            }

            test("renders an updated message on update") {
                val outcome = AddOutcome(newContent = "", vulnId = VulnId.Cve("CVE-2026-1234"), updated = true)

                formatAddOutcomeMessage(Path.of("/tmp/x.vl.yaml"), outcome) shouldBe
                    "Updated in /tmp/x.vl.yaml: CVE-2026-1234"
            }
        }
    })
