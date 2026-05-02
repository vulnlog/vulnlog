// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class CopyTest :
    FunSpec({

        test("latestPublishedRelease returns last published release") {
            val releases =
                listOf(
                    ReleaseEntry(id = Release("1.0.0"), publicationDate = LocalDate.of(2025, 1, 1)),
                    ReleaseEntry(id = Release("1.1.0"), publicationDate = LocalDate.of(2025, 6, 1)),
                    ReleaseEntry(id = Release("1.2.0")),
                )

            lastReleaseFavoringPublished(releases) shouldBe Release("1.1.0")
        }

        test("latestPublishedRelease returns null when no releases are published") {
            val releases =
                listOf(
                    ReleaseEntry(id = Release("1.0.0")),
                    ReleaseEntry(id = Release("1.1.0")),
                )

            lastReleaseFavoringPublished(releases) shouldBe Release("1.1.0")
        }

        test("serializeEntryYaml produces correctly indented YAML list item") {
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

        test("serializeEntryYaml omits null fields") {
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

        test("serializeEntryYaml omits empty lists") {
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

        test("insertEntryAfterVulnerabilitiesHeader inserts after header") {
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

        test("insertEntryAfterVulnerabilitiesHeader handles empty vulnerabilities list") {
            val fileContent =
                """
                |vulnerabilities: []
                """.trimMargin()

            val entryYaml = "  - id: \"CVE-2026-9999\"\n    releases:\n      - \"1.0.0\""

            val result = insertEntryAfterVulnerabilitiesHeader(fileContent, entryYaml)

            result shouldContain "vulnerabilities:"
            result shouldNotContain "[]"
            result shouldContain "CVE-2026-9999"
        }
    })
