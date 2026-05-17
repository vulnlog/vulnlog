// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.TagEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
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

            yaml shouldStartWith "  - id: \"CVE-2026-1234\""
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
            yaml shouldContain "\"1.0.0\""
            yaml shouldContain "pkg:npm/example-lib@2.3.0"
            yaml shouldContain "frontend"
            yaml shouldContain "reporter: \"trivy\""
            yaml shouldContain "at: \"${LocalDate.now()}\""
        }

        context("addVulnerabilityToFile") {

            test("inserts the entry and falls back to the latest published release") {
                val file = vulnlogFile()
                val outcome = addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS)

                outcome.vulnId shouldBe VulnId.Cve("CVE-2026-1234")
                outcome.newContent shouldContain "CVE-2026-1234"
                outcome.newContent shouldContain "\"1.0.0\""
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

                outcome.newContent shouldContain "\"1.0.0\""
            }

            test("throws when the vuln id already exists") {
                val existing =
                    VulnerabilityEntry(
                        id = VulnId.Cve("CVE-2026-1234"),
                        releases = listOf(Release("1.0.0")),
                        packages = emptyList(),
                        reports = emptyList(),
                        verdict = Verdict.UnderInvestigation,
                    )
                val file = vulnlogFile(vulnerabilities = listOf(existing))

                shouldThrow<IllegalArgumentException> {
                    addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS)
                }.message shouldContain "already exists"
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

            test("throws when no releases are defined and none supplied") {
                val file = vulnlogFile(releases = emptyList())

                shouldThrow<IllegalArgumentException> {
                    addVulnerabilityToFile(file, renderContent(file), DEFAULT_OPTIONS)
                }.message shouldContain "no releases"
            }
        }
    })
