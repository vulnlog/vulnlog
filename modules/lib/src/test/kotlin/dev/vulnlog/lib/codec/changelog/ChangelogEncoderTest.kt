// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.changelog

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ChangelogReport
import dev.vulnlog.lib.core.reporting.summarize
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
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
) = ChangelogEntry(
    primaryId = primaryId,
    aliases = aliases,
    name = name,
    description = description,
    impact = impact,
    note = note,
    ref = ref,
)

private fun changelogOf(
    entries: List<ChangelogEntry> = listOf(entry()),
    publishedAt: LocalDate? = LocalDate.parse("2024-02-01"),
) = Changelog(
    project = Project("Acme Corp", "Acme Web App", "Acme Security"),
    releases =
        listOf(
            ChangelogRelease(
                fixedIn = release("1.1.0"),
                publishedAt = publishedAt,
                summary = summarize(entries),
                entries = entries,
            ),
        ),
)

private fun reportOf(
    changelog: Changelog = changelogOf(),
    filter: ResolvedFilter = ResolvedFilter(),
    asOf: Release? = null,
) = ChangelogReport(
    changelog = changelog,
    generatedAt = Instant.parse("2026-01-15T10:30:00Z"),
    vulnlogVersion = "1.2.3",
    inputs = listOf("frontend.vl", "backend.vl"),
    filter = filter,
    asOf = asOf,
)

private fun ChangelogReport.json(): JsonNode = JsonMapper.shared().readTree(ChangelogEncoder.encodeJson(this))

private fun JsonNode.texts(): List<String> = values().map { it.stringValue() }

class ChangelogEncoderTest :
    FunSpec({

        context("text") {

            test("renders the project, the release, and what it fixed") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

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
                val changelog = changelogOf(publishedAt = null)

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "1.1.0 (unreleased)"
            }

            test("names aliases alongside the primary identifier rather than beside it") {
                val changelog = changelogOf(entries = listOf(entry(aliases = setOf(ghsa("GHSA-aaaa-bbbb-cccc")))))

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "CVE-2024-0001 (high, also GHSA-aaaa-bbbb-cccc)"
            }

            test("renders the common name of a vulnerability") {
                val changelog = changelogOf(entries = listOf(entry(name = "Log4Shell")))

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

                rendered shouldContain """CVE-2024-0001 (high) "Log4Shell" SQL injection in the parser"""
            }

            test("omits the fix and reference lines when the resolution records neither") {
                val changelog = changelogOf(entries = listOf(entry(note = null, ref = null)))

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

                rendered shouldNotContain "fix:"
            }

            test("states that a vulnerability was judged not affected") {
                val changelog =
                    changelogOf(
                        entries =
                            listOf(
                                entry(impact = Impact.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT)),
                            ),
                    )

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.FULL)

                rendered shouldContain "CVE-2024-0001 (not affected)"
            }

            test("drops the description and the resolution details when brief") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeText(changelog, ChangelogDetail.BRIEF)

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
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().take(3) shouldBe listOf("## [1.1.0] - 2024-02-01", "", "### Security")
            }

            test("renders the description, the fix, and the reference as one bullet") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().last() shouldBe
                    "- **CVE-2024-0001** (high): SQL injection in the parser. " +
                    "Updated parser 1.0 to 1.1. (https://issues.example.com/SEC-1)"
            }

            test("omits the project so the sections paste under a heading the changelog already has") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.FULL)

                rendered shouldNotContain "Acme Corp"
            }

            test("marks a release without a publication date as unreleased") {
                val changelog = changelogOf(publishedAt = null)

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.FULL)

                rendered shouldContain "## [1.1.0] - unreleased"
            }

            test("keeps each fix on one line so it renders as one bullet") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.FULL)

                rendered.lines().count { it.startsWith("- ") } shouldBe 1
            }

            test("drops the description and the resolution details when brief") {
                val changelog = changelogOf()

                val rendered = ChangelogEncoder.encodeMarkdown(changelog, ChangelogDetail.BRIEF)

                rendered shouldContain "- **CVE-2024-0001** (high)"
            }
        }
        context("json") {

            test("names the format version, the project, the Vulnlog version, the inputs and the generation time") {
                val json = reportOf().json()

                json["format_version"].intValue() shouldBe 1
                json["project"]["organization"].stringValue() shouldBe "Acme Corp"
                json["project"]["name"].stringValue() shouldBe "Acme Web App"
                json["project"]["author"].stringValue() shouldBe "Acme Security"
                json["vulnlog_version"].stringValue() shouldBe "1.2.3"
                json["inputs"].texts() shouldContainExactly listOf("frontend.vl", "backend.vl")
                json["generated_at"].stringValue() shouldBe "2026-01-15T10:30:00Z"
            }

            test("writes each release with its date, its summary and every field of its entries") {
                val withAlias = entry(aliases = setOf(ghsa("GHSA-abcd-1234-efgh")), name = "ParserBleed")

                val release = reportOf(changelogOf(listOf(withAlias))).json()["releases"].single()

                release["release"].stringValue() shouldBe "1.1.0"
                release["published_at"].stringValue() shouldBe "2024-02-01"
                release["summary"]["total"].intValue() shouldBe 1
                release["summary"]["by_severity"]["high"].intValue() shouldBe 1
                val entry = release["entries"].single()
                entry["primary_id"].stringValue() shouldBe "CVE-2024-0001"
                entry["aliases"].texts() shouldContainExactly listOf("GHSA-abcd-1234-efgh")
                entry["name"].stringValue() shouldBe "ParserBleed"
                entry["description"].stringValue() shouldBe "SQL injection in the parser"
                entry["verdict"].stringValue() shouldBe "affected"
                entry["severity"].stringValue() shouldBe "high"
                entry["justification"].isNull shouldBe true
                entry["note"].stringValue() shouldBe "Updated parser 1.0 to 1.1"
                entry["ref"].stringValue() shouldBe "https://issues.example.com/SEC-1"
            }

            test("writes a null date for a release that is not published") {
                val changelog = changelogOf(publishedAt = null)

                val release = reportOf(changelog).json()["releases"].single()

                release["published_at"].isNull shouldBe true
            }

            test("names the applied filter under its canonical tokens, the tags sorted") {
                val filter =
                    ResolvedFilter(
                        reporter = ReporterType.TRIVY,
                        tags = setOf(Tag("production"), Tag("frontend")),
                        fixedIn = release("1.1.0"),
                    )

                val json = reportOf(filter = filter, asOf = release("1.1.0")).json()["filter"]

                json["as_of"].stringValue() shouldBe "1.1.0"
                json["tags"].texts() shouldContainExactly listOf("frontend", "production")
                json["reporter"].stringValue() shouldBe "trivy"
                json["fixed_in"].stringValue() shouldBe "1.1.0"
            }

            test("writes an unset filter as null values and empty lists") {
                val json = reportOf().json()["filter"]

                json["as_of"].isNull shouldBe true
                json["tags"].isEmpty shouldBe true
                json["reporter"].isNull shouldBe true
                json["fixed_in"].isNull shouldBe true
            }

            test("writes the releases without fixes as an empty list") {
                val empty = Changelog(Project("Acme Corp", "Acme Web App", "Acme Security"), emptyList())

                val json = reportOf(empty).json()

                json["releases"].isEmpty shouldBe true
            }
        }
    })
