// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog

import dev.vulnlog.lib.codec.readOrUpdateGolden
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ChangelogReport
import dev.vulnlog.lib.core.reporting.summarize
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.Changelog
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.ChangelogEntry
import dev.vulnlog.lib.model.reporting.ChangelogRelease
import dev.vulnlog.lib.model.reporting.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

class ChangelogGoldenTest :
    FunSpec({
        test("matches golden Markdown snapshot") {
            val actual = ChangelogEncoder.encodeMarkdown(goldenFixture(), ChangelogDetail.FULL)

            actual shouldBe readOrUpdateGolden("golden-changelog.md", actual)
        }

        test("matches golden JSON snapshot") {
            val report =
                ChangelogReport(
                    changelog = goldenFixture(),
                    generatedAt = Instant.parse("2026-05-02T10:30:00Z"),
                    vulnlogVersion = "1.2.3-test",
                    inputs = listOf("frontend.vl", "backend.vl"),
                    filter = ResolvedFilter(reporter = ReporterType.TRIVY, tags = setOf(Tag("production"))),
                    asOf = Release("1.2.0"),
                )

            val actual = ChangelogEncoder.encodeJson(report)

            actual shouldBe readOrUpdateGolden("golden-changelog.json", actual)
        }
    })

private val unreleasedEntries =
    listOf(
        ChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-1111"),
            aliases = setOf(VulnId.Ghsa("GHSA-aaaa-bbbb-cccc")),
            name = "Log4Shell",
            description = "Remote code execution in example-lib",
            impact = Impact.Affected(Severity.HIGH),
            note = "Updated example-lib from 2.3.0 to 2.4.0",
            ref = "https://issues.example.com/SEC-1",
        ),
        ChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-2222"),
            impact = Impact.Affected(Severity.LOW),
            description = "Denial of service via large payload",
        ),
    )

private val publishedEntries =
    listOf(
        ChangelogEntry(
            primaryId = VulnId.Cve("CVE-2026-3333"),
            impact = Impact.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT),
            note = "Updated the dependency anyway.",
        ),
    )

private fun goldenFixture() =
    Changelog(
        project = Project("Acme Corp", "Acme Web App", "Security Team"),
        releases =
            listOf(
                ChangelogRelease(
                    fixedIn = Release("1.2.0"),
                    publishedAt = null,
                    summary = summarize(unreleasedEntries),
                    entries = unreleasedEntries,
                ),
                ChangelogRelease(
                    fixedIn = Release("1.1.0"),
                    publishedAt = LocalDate.parse("2026-03-20"),
                    summary = summarize(publishedEntries),
                    entries = publishedEntries,
                ),
            ),
    )
