// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.core.reporting.formatChangelogMarkdown
import dev.vulnlog.lib.core.reporting.formatChangelogText
import dev.vulnlog.lib.core.reporting.summarize
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ReportingChangelogEntry
import dev.vulnlog.lib.model.reporting.ReportingChangelogProject
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

private val first = cve("CVE-2024-0001")

private fun entry(
    primaryId: VulnId = first,
    aliases: Set<VulnId> = emptySet(),
    name: String? = null,
    description: String? = "SQL injection in the parser",
    impact: Impact = Impact.Affected(Severity.HIGH),
    note: String? = "Updated parser 1.0 to 1.1",
    ref: String? = "https://issues.example.com/SEC-1",
) = ReportingChangelogEntry(
    primaryId = primaryId,
    aliases = aliases,
    name = name,
    description = description,
    impact = impact,
    note = note,
    ref = ref,
)

private fun report(
    entries: List<ReportingChangelogEntry> = listOf(entry()),
    publishedAt: LocalDate? = LocalDate.parse("2024-02-01"),
) = ReportingChangelogProject(
    project = Project("Acme Corp", "Acme Web App", "Acme Security"),
    releases =
        listOf(
            ReportingChangelogRelease(
                fixedIn = release("1.1.0"),
                publishedAt = publishedAt,
                summary = summarize(entries),
                entries = entries,
            ),
        ),
)

class ReportingChangelogFormatterTest :
    FunSpec({

        context("text") {

            test("renders the project, the release, and what it fixed") {
                val changelog = report()

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldBe
                    """
                    Acme Corp / Acme Web App

                    1.1.0 (2024-02-01)
                      1 fixed: 1 high
                      CVE-2024-0001 (high) SQL injection in the parser
                        fix: Updated parser 1.0 to 1.1
                        ref: https://issues.example.com/SEC-1
                    """.trimIndent()
            }

            test("marks a release without a publication date as unreleased") {
                val changelog = report(publishedAt = null)

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "1.1.0 (unreleased)"
            }

            test("names aliases alongside the primary identifier rather than beside it") {
                val changelog = report(entries = listOf(entry(aliases = setOf(ghsa("GHSA-aaaa-bbbb-cccc")))))

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "CVE-2024-0001 (high, also GHSA-aaaa-bbbb-cccc)"
            }

            test("renders the common name of a vulnerability") {
                val changelog = report(entries = listOf(entry(name = "Log4Shell")))

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldContain """CVE-2024-0001 (high) "Log4Shell" SQL injection in the parser"""
            }

            test("omits the fix and reference lines when the resolution records neither") {
                val changelog = report(entries = listOf(entry(note = null, ref = null)))

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldNotContain "fix:"
            }

            test("states that a vulnerability was judged not affected") {
                val changelog =
                    report(
                        entries = listOf(entry(impact = Impact.NotAffected("Vulnerable code not present"))),
                    )

                val rendered = formatChangelogText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "CVE-2024-0001 (not affected)"
            }

            test("drops the description and the resolution details when brief") {
                val changelog = report()

                val rendered = formatChangelogText(changelog, ChangelogDetail.BRIEF)

                rendered shouldBe
                    """
                    Acme Corp / Acme Web App

                    1.1.0 (2024-02-01)
                      1 fixed: 1 high
                      CVE-2024-0001 (high)
                    """.trimIndent()
            }
        }

        context("markdown") {

            test("heads each release with its version and date, followed by a Security section") {
                val changelog = report()

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().take(3) shouldBe listOf("## [1.1.0] - 2024-02-01", "", "### Security")
            }

            test("renders the description, the fix, and the reference as one bullet") {
                val changelog = report()

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().last() shouldBe
                    "- **CVE-2024-0001** (high): SQL injection in the parser. " +
                    "Updated parser 1.0 to 1.1. (https://issues.example.com/SEC-1)"
            }

            test("omits the project so the sections paste under a heading the changelog already has") {
                val changelog = report()

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.FULL)

                rendered shouldNotContain "Acme Corp"
            }

            test("marks a release without a publication date as unreleased") {
                val changelog = report(publishedAt = null)

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.FULL)

                rendered shouldContain "## [1.1.0] - unreleased"
            }

            test("keeps each fix on one line so it renders as one bullet") {
                val changelog = report()

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().count { it.startsWith("- ") } shouldBe 1
            }

            test("drops the description and the resolution details when brief") {
                val changelog = report()

                val rendered = formatChangelogMarkdown(changelog, ChangelogDetail.BRIEF)

                rendered shouldContain "- **CVE-2024-0001** (high)"
            }
        }
    })
