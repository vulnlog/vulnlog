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
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
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

private fun render(file: VulnlogFile): VulnlogFileRaw = VulnlogFileRaw(YamlWriter.write(file, createYamlMapper()))

class CopyTest :
    FunSpec({

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
                    "Copied: 2 entries to /tmp/x.vl.yaml"
            }

            test("renders no-op message when empty") {
                formatCopiedMessage(Path.of("/tmp/x.vl.yaml"), emptyList()) shouldBe
                    "Unchanged: /tmp/x.vl.yaml: no new vulnerabilities"
            }
        }

        context("formatVulnIdsNotInSourceMessage") {

            test("renders missing ids") {
                formatVulnIdsNotInSourceMessage(setOf(cve1, ghsa1)) shouldContain
                    "error: vulnerability IDs not found in source file:"
                formatVulnIdsNotInSourceMessage(setOf(cve1)) shouldContain "CVE-2026-1234"
            }
        }

        context("copyVulnerabilities") {

            test("inserts an entry that does not exist in the destination") {
                val source =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(id = cve1, description = "from source")),
                    )
                val destination = vulnlogFile()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                outcome.copied shouldContainExactly listOf(cve1)
                outcome.newContent.content shouldContain "CVE-2026-1234"
                outcome.newContent.content shouldContain "from source"
                // release rewritten to destination's latest release (1.0.0)
                outcome.newContent.content shouldContain "releases: [1.0.0]"
                outcome.newContent.content shouldNotContain "2.0.0"
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
                                    analysis = null, // null in existing -> falls back to source
                                    name = null, // null in existing -> falls back to source
                                    releases = listOf(release1),
                                ),
                            ),
                    )

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                outcome.copied shouldContainExactly listOf(cve1)
                outcome.newContent.content shouldContain "existing description"
                outcome.newContent.content shouldNotContain "source description"
                outcome.newContent.content shouldContain "source analysis"
                outcome.newContent.content shouldContain "source name"
                // exactly one entry - replace, not duplicate
                "CVE-2026-1234".toRegex().findAll(outcome.newContent.content).count() shouldBe 1
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

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                // packages: union (existing first, then unique additions)
                outcome.newContent.content shouldContain "pkg:npm/dest-only@1.0"
                outcome.newContent.content shouldContain "pkg:npm/source-only@1.0"
                // aliases: source's alias appears
                outcome.newContent.content shouldContain "GHSA-1234-5678-abcd"
            }

            test("rewrites releases to destination's latest release") {
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
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                val entryBody = outcome.newContent.content.substring(outcome.newContent.content.indexOf("- id: CVE"))
                entryBody shouldContain "releases: [1.5.0]"
                entryBody shouldNotContain "1.0.0"
                entryBody shouldNotContain "2.0.0"
            }

            test("renders a multi-line source analysis as a literal block scalar") {
                val analysisText = "Affected paths:\n  - decode()\nNone reachable."
                val source =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(id = cve1, analysis = analysisText)),
                    )
                val destination = vulnlogFile()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                outcome.newContent.content shouldContain "analysis: |-"
                outcome.newContent.content shouldNotContain "analysis: >"
            }

            test("normalizes a column-0 destination and re-formatting is a no-op") {
                val source =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(id = cve2, description = "from source")),
                    )
                val existing = vulnerability(id = cve1, releases = listOf(release1))
                val destination = vulnlogFile(vulnerabilities = listOf(existing))
                val destinationContent =
                    VulnlogFileRaw(
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
                        """.trimMargin() + "\n",
                    )
                val mapper = createYamlMapper()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(destinationContent),
                        vulnIds = setOf(cve2),
                        mapper = mapper,
                    )

                outcome.newContent.content shouldContain "vulnerabilities:\n\n  - id: CVE-2026-5678"
                outcome.newContent.content shouldContain "releases:\n  - id: 1.0.0"
                "CVE-2026-1234".toRegex().findAll(outcome.newContent.content).count() shouldBe 1
                formatYaml(parsed(outcome.newContent), mapper).content shouldBe outcome.newContent.content
            }

            test("preserves the schema header when the destination has one") {
                val source =
                    vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1, description = "from source")))
                val destination = vulnlogFile()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        // render uses YamlWriter.write, which emits the '# $schema:' header
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve1),
                    )

                outcome.newContent.content shouldStartWith "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---"
            }

            test("does not add a schema header when the destination has none") {
                val source =
                    vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1, description = "from source")))
                val destination = vulnlogFile()
                val destinationContent =
                    VulnlogFileRaw(
                        """
                        |---
                        |schemaVersion: "1"
                        |
                        |project:
                        |  organization: Acme
                        |  name: App
                        |  author: Sec
                        |
                        |releases:
                        |  - id: 1.0.0
                        |    published_at: 2026-01-01
                        |
                        |vulnerabilities: []
                        """.trimMargin() + "\n",
                    )

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(destinationContent),
                        vulnIds = setOf(cve1),
                    )

                outcome.newContent.content shouldNotContain "# \$schema:"
                outcome.newContent.content shouldStartWith "---"
            }

            test("ignores ids in vulnIds that are not present in the source") {
                val source = vulnlogFile(vulnerabilities = listOf(vulnerability(id = cve1)))
                val destination = vulnlogFile()

                val outcome =
                    copyVulnerabilities(
                        source = source,
                        destination = parsed(render(destination)),
                        vulnIds = setOf(cve2), // not in source
                    )

                outcome.copied shouldBe emptyList()
                outcome.newContent.content shouldNotContain "CVE-2026-5678"
            }
        }
    })
