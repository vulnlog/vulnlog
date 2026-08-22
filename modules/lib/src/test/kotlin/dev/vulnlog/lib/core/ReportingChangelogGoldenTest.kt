// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.core.reporting.formatChangelogMarkdown
import dev.vulnlog.lib.core.reporting.summarize
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ReportingChangelogEntry
import dev.vulnlog.lib.model.reporting.ReportingChangelogProject
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

private const val GOLDEN_RESOURCE = "/report/golden-changelog.md"
private val GOLDEN_SOURCE: Path = Path.of("src/test/resources/report/golden-changelog.md")

class ReportingChangelogGoldenTest :
    FunSpec({
        test("matches golden Markdown snapshot") {
            val actual = formatChangelogMarkdown(goldenFixture(), ChangelogDetail.FULL)

            if (shouldUpdateGolden()) {
                Files.createDirectories(GOLDEN_SOURCE.parent)
                Files.writeString(GOLDEN_SOURCE, actual)
                println("Golden Markdown updated at $GOLDEN_SOURCE")
                return@test
            }

            val expected =
                ReportingChangelogGoldenTest::class.java
                    .getResourceAsStream(GOLDEN_RESOURCE)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error(
                        "Golden Markdown missing at classpath $GOLDEN_RESOURCE. " +
                            "Run with UPDATE_GOLDEN=1 to create it.",
                    )

            actual shouldBe expected
        }
    })

private fun shouldUpdateGolden(): Boolean = System.getenv("UPDATE_GOLDEN") in listOf("1", "true")

private val unreleasedEntries =
    listOf(
        ReportingChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-1111"),
            aliases = setOf(VulnId.Ghsa("GHSA-aaaa-bbbb-cccc")),
            name = "Log4Shell",
            description = "Remote code execution in example-lib",
            impact = Impact.Affected(Severity.HIGH),
            note = "Updated example-lib from 2.3.0 to 2.4.0",
            ref = "https://issues.example.com/SEC-1",
        ),
        ReportingChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-2222"),
            impact = Impact.Affected(Severity.LOW),
            description = "Denial of service via large payload",
        ),
    )

private val publishedEntries =
    listOf(
        ReportingChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-3333"),
            impact = Impact.NotAffected("Vulnerable code not present"),
            note = "Updated the dependency anyway.",
        ),
    )

private fun goldenFixture() =
    ReportingChangelogProject(
        project = Project("Acme Corp", "Acme Web App", "Security Team"),
        releases =
            listOf(
                ReportingChangelogRelease(
                    fixedIn = Release("1.2.0"),
                    publishedAt = null,
                    summary = summarize(unreleasedEntries),
                    entries = unreleasedEntries,
                ),
                ReportingChangelogRelease(
                    fixedIn = Release("1.1.0"),
                    publishedAt = LocalDate.parse("2026-03-20"),
                    summary = summarize(publishedEntries),
                    entries = publishedEntries,
                ),
            ),
    )
