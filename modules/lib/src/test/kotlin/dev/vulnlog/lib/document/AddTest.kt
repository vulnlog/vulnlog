// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import java.time.LocalDate

private val TODAY = LocalDate.of(2026, 5, 4)

private val DEFAULT_OPTIONS = AddVulnerabilityOptions(vulnId = VulnId.Cve("CVE-2026-1234"))

private val TWO_RELEASES =
    listOf(
        ReleaseEntry(Release("1.0.0"), publicationDate = LocalDate.of(2026, 1, 15)),
        ReleaseEntry(Release("2.0.0"), publicationDate = LocalDate.of(2026, 3, 1)),
    )

/** Rendered with `YamlWriter.write`, so the document starts with the `# $schema:` header. */
private fun content(
    releases: List<ReleaseEntry> = listOf(ReleaseEntry(Release("1.0.0"), publicationDate = LocalDate.of(2026, 1, 15))),
    tags: List<TagEntry> = emptyList(),
): String =
    YamlWriter.write(
        VulnlogFile(
            schemaVersion = SchemaVersion.V1,
            project = Project("acme", "widget", "alice"),
            tags = tags,
            releases = releases,
            vulnerabilities = emptyList(),
        ),
    )

/** Quoted values and block lists, without a schema header, so a test can see the output become canonical. */
private fun yamlWithEntries(
    entriesYaml: String,
    tagIds: List<String> = emptyList(),
): String {
    val tagsSection = if (tagIds.isEmpty()) "" else "tags:\n" + tagIds.joinToString("") { "  - id: \"$it\"\n" } + "\n"
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
        |
        """.trimMargin()
    val releasesAndEntries =
        """
        |releases:
        |  - id: "1.0.0"
        |    published_at: "2026-01-15"
        |
        |vulnerabilities:
        |
        """.trimMargin()
    return header + tagsSection + releasesAndEntries + entriesYaml + "\n"
}

private fun occurrences(
    text: String,
    part: String,
): Int = text.windowed(part.length).count { it == part }

class AddTest :
    FunSpec({

        context("createVulnerabilityEntry") {

            test("leaves the list fields empty and sets no verdict when only an id is given") {
                val yaml = createVulnerabilityEntry(DEFAULT_OPTIONS, TODAY)

                yaml shouldStartWith "  - id: CVE-2026-1234"
                yaml shouldContain "releases: []"
                yaml shouldContain "packages: []"
                yaml shouldContain "reports: []"
                yaml shouldNotContain "verdict"
                yaml shouldNotContain "tags"
            }

            test("lists releases, packages and tags, and dates each reporter's report today") {
                val options =
                    DEFAULT_OPTIONS.copy(
                        releases = setOf(Release("1.0.0")),
                        packages = setOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
                        tags = setOf(Tag("frontend")),
                        reporters = setOf(ReporterType.TRIVY),
                    )

                val yaml = createVulnerabilityEntry(options, TODAY)

                yaml shouldContain "releases: [1.0.0]"
                yaml shouldContain "pkg:npm/example-lib@2.3.0"
                yaml shouldContain "frontend"
                yaml shouldContain "reporter: trivy"
                yaml shouldContain "at: 2026-05-04"
            }

            test("writes the scalar fields as given, even an inconsistent verdict and justification") {
                val options =
                    DEFAULT_OPTIONS.copy(
                        name = "Log4Shell",
                        aliases = setOf(VulnId.Ghsa("GHSA-1234-5678-abcd")),
                        description = "Remote code execution.",
                        analysis = "Not reachable in our usage.",
                        analyzedAt = LocalDate.of(2026, 2, 1),
                        verdict = "affected",
                        severity = "high",
                        justification = "vulnerable code not in execute path",
                        comment = "Revisit after the next upgrade.",
                    )

                val yaml = createVulnerabilityEntry(options, TODAY)

                yaml shouldContain "name: Log4Shell"
                yaml shouldContain "GHSA-1234-5678-abcd"
                yaml shouldContain "description: Remote code execution."
                yaml shouldContain "analysis: Not reachable in our usage."
                yaml shouldContain "analyzed_at: 2026-02-01"
                yaml shouldContain "verdict: affected"
                yaml shouldContain "severity: high"
                yaml shouldContain "justification: vulnerable code not in execute path"
                yaml shouldContain "comment: Revisit after the next upgrade."
            }
        }

        context("addVulnerabilityToFile") {

            test("gives a new entry without a release the latest release of the file") {
                val destination = validated(content(releases = TWO_RELEASES))

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                edit.updated shouldBe false
                edit.newContent shouldContain "releases: [2.0.0]"
            }

            test("gives a new entry the release it names") {
                val destination = validated(content(releases = TWO_RELEASES))

                val edit =
                    addVulnerabilityToFile(destination, DEFAULT_OPTIONS.copy(releases = setOf(Release("1.0.0"))), TODAY)

                edit.newContent shouldContain "releases: [1.0.0]"
            }

            test("leaves a new entry without a release when the file declares none") {
                val destination = validated(content(releases = emptyList()))

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                edit.newContent.substringAfter("- id: CVE-2026-1234") shouldContain "releases: []"
            }

            test("attaches a tag the file declares") {
                val destination = validated(content(tags = listOf(TagEntry(Tag("frontend")))))

                val edit =
                    addVulnerabilityToFile(destination, DEFAULT_OPTIONS.copy(tags = setOf(Tag("frontend"))), TODAY)

                edit.newContent shouldContain "tags: [frontend]"
            }

            test("dates an existing reporter's report today and adds a report for a new one") {
                val destination =
                    validated(
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
                        ),
                    )
                val options = DEFAULT_OPTIONS.copy(reporters = setOf(ReporterType.TRIVY, ReporterType.SNYK))

                val edit = addVulnerabilityToFile(destination, options, TODAY)

                edit.updated shouldBe true
                edit.newContent shouldContain "reporter: trivy"
                edit.newContent shouldContain "reporter: snyk"
                occurrences(edit.newContent, "at: 2026-05-04") shouldBe 2
                edit.newContent shouldNotContain "2026-01-10"
            }

            test("rewrites a document in any layout canonically, so fmt changes nothing") {
                val destination =
                    validated(
                        """
                        |schemaVersion: "1"
                        |project:
                        |  organization: acme
                        |  name: widget
                        |  author: alice
                        |releases:
                        |- id: 1.0.0
                        |  published_at: 2026-01-15
                        |vulnerabilities:
                        |- id: CVE-2026-0001
                        |  releases: [1.0.0]
                        |  packages: []
                        |  reports: []
                        """.trimMargin() + "\n",
                    )

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                edit.newContent shouldContain "vulnerabilities:\n\n  - id: CVE-2026-1234"
                edit.newContent shouldContain "releases:\n  - id: 1.0.0"
                occurrences(edit.newContent, "CVE-2026-0001") shouldBe 1
                formatYaml(parsed(edit.newContent)) shouldBe edit.newContent
            }

            test("keeps the schema header when the destination has one") {
                val destination = validated(content())

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                edit.newContent shouldStartWith "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---"
            }

            test("adds no schema header when the destination has none") {
                val destination =
                    validated(
                        yamlWithEntries(
                            """
                            |  - id: "CVE-2026-0001"
                            |    releases:
                            |      - "1.0.0"
                            |    packages: []
                            |    reports: []
                            """.trimMargin(),
                        ),
                    )

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                edit.newContent shouldStartWith "---"
                edit.newContent shouldNotContain "# \$schema:"
            }

            test("adds to the lists of an existing entry and keeps its other fields") {
                val destination =
                    validated(
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
                            tagIds = listOf("frontend"),
                        ),
                    )
                val options =
                    DEFAULT_OPTIONS.copy(
                        packages = setOf(Purl.Npm("pkg:npm/new-lib@2.0.0"), Purl.Npm("pkg:npm/old-lib@1.0.0")),
                    )

                val edit = addVulnerabilityToFile(destination, options, TODAY)

                edit.updated shouldBe true
                edit.newContent shouldContain "pkg:npm/new-lib@2.0.0"
                occurrences(edit.newContent, "pkg:npm/old-lib@1.0.0") shouldBe 1
                edit.newContent shouldContain "Existing Name"
                edit.newContent shouldContain "Existing description."
                edit.newContent shouldContain "frontend"
                edit.newContent shouldContain "verdict: affected"
                edit.newContent shouldContain "severity: high"
                edit.newContent shouldContain "Existing comment."
            }

            test("overwrites the values it is given on an existing entry") {
                val destination =
                    validated(
                        yamlWithEntries(
                            """
                            |  - id: "CVE-2026-1234"
                            |    description: "Old description."
                            |    releases:
                            |      - "1.0.0"
                            |    packages: []
                            |    reports: []
                            |    verdict: affected
                            |    severity: high
                            """.trimMargin(),
                        ),
                    )
                val options = DEFAULT_OPTIONS.copy(description = "New description.", severity = "low")

                val edit = addVulnerabilityToFile(destination, options, TODAY)

                val entry = edit.newContent.substringAfter("- id: CVE-2026-1234")
                entry shouldContain "description: New description."
                entry shouldContain "verdict: affected"
                entry shouldContain "severity: low"
                entry shouldNotContain "Old description."
            }

            test("keeps the releases of an existing entry when none is named") {
                val destination =
                    validated(
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
                        """.trimMargin(),
                    )

                val edit = addVulnerabilityToFile(destination, DEFAULT_OPTIONS, TODAY)

                val entry = edit.newContent.substringAfter("- id: CVE-2026-1234")
                edit.updated shouldBe true
                entry shouldContain "releases: [1.0.0]"
                entry shouldNotContain "2.0.0"
            }

            test("keeps an updated entry in its place and does not duplicate it") {
                val destination =
                    validated(
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
                        ),
                    )
                val options = DEFAULT_OPTIONS.copy(packages = setOf(Purl.Npm("pkg:npm/lib@1.0.0")))

                val edit = addVulnerabilityToFile(destination, options, TODAY)

                edit.newContent.indexOf("CVE-2026-0001") shouldBeLessThan edit.newContent.indexOf("CVE-2026-1234")
                occurrences(edit.newContent, "CVE-2026-0001") shouldBe 1
                occurrences(edit.newContent, "CVE-2026-1234") shouldBe 1
            }

            test("writes a multi-line description of an updated entry as a literal block") {
                val destination =
                    validated(
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
                        ),
                    )
                val options = DEFAULT_OPTIONS.copy(packages = setOf(Purl.Npm("pkg:npm/lib@1.0.0")))

                val edit = addVulnerabilityToFile(destination, options, TODAY)

                edit.newContent shouldContain "description: |-"
                edit.newContent shouldNotContain "description: >"
            }
        }
    })
