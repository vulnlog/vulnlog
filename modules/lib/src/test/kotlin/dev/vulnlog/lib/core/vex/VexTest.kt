// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private val affected = Verdict.Affected(Severity.HIGH)

/** Four releases in order; 1.2.0 carries a publication date so a fix without a date can fall back to it. */
private val file: VulnlogFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry("1.0.0"),
                releaseEntry("1.1.0"),
                releaseEntry("1.2.0", publishedAt = LocalDate.of(2026, 4, 20)),
                releaseEntry("1.3.0"),
            ),
    )

private fun affectedIn(
    releases: List<String>,
    disposition: Disposition? = null,
    fixedIn: String? = null,
    note: String? = null,
    fixedAt: LocalDate? = null,
) = vulnerability(
    id = cve("CVE-2026-1234"),
    releases = releases.map(::release),
    verdict = Verdict.Affected(Severity.HIGH, disposition),
    resolution = fixedIn?.let { resolution(release = it, note = note, at = fixedAt) },
)

private fun statusesOf(entry: dev.vulnlog.lib.model.VulnerabilityEntry) =
    releaseStatuses(entry, file).map { it.release.value to it.status }

class VexTest :
    FunSpec({

        context("releaseStatuses") {

            test("an entry applies from the first release it lists until the fix, and is fixed from then on") {
                val entry = affectedIn(releases = listOf("1.1.0"), fixedIn = "1.2.0")

                val statuses = releaseStatuses(entry, file)

                statuses.map { it.release } shouldContainExactly
                    listOf(release("1.1.0"), release("1.2.0"), release("1.3.0"))
                statuses[0].status.shouldBeInstanceOf<VexStatus.Affected>()
                statuses[1].status shouldBe VexStatus.Fixed
                statuses[2].status shouldBe VexStatus.Fixed
            }

            test("without a resolution every later release stays affected") {
                val entry = affectedIn(releases = listOf("1.1.0"))

                statusesOf(entry).map { it.first } shouldContainExactly listOf("1.1.0", "1.2.0", "1.3.0")
                statusesOf(entry).all { it.second is VexStatus.Affected } shouldBe true
            }

            test("the earliest listed release opens the range") {
                val entry = affectedIn(releases = listOf("1.2.0", "1.0.0"))

                statusesOf(entry).map { it.first } shouldContainExactly listOf("1.0.0", "1.1.0", "1.2.0", "1.3.0")
            }

            test("a fix declared before the first listed release wins from the fix on") {
                val entry = affectedIn(releases = listOf("1.2.0"), fixedIn = "1.1.0")

                val statuses = releaseStatuses(entry, file)

                statuses.map { it.release.value } shouldContainExactly listOf("1.1.0", "1.2.0", "1.3.0")
                statuses.all { it.status == VexStatus.Fixed } shouldBe true
            }

            test("an entry listing no release applies nowhere") {
                val entry = vulnerability(id = cve("CVE-2026-1234"), verdict = affected)

                releaseStatuses(entry, file).shouldBeEmpty()
            }

            test("the verdict decides the status before the fix") {
                val notAffected =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.0.0")),
                        verdict = Verdict.NotAffected(dev.vulnlog.lib.model.VexJustification.COMPONENT_NOT_PRESENT),
                    )
                val open = vulnerability(id = cve("CVE-2026-1234"), releases = listOf(release("1.3.0")))

                releaseStatuses(notAffected, file).first().status.shouldBeInstanceOf<VexStatus.NotAffected>()
                releaseStatuses(open, file).single().status shouldBe VexStatus.UnderInvestigation(null)
            }
        }

        context("releaseStatuses dates") {

            test("a verdict is dated by the analysis date, else the first report") {
                val analyzed =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 1))),
                        analyzedAt = LocalDate.of(2026, 4, 6),
                        verdict = affected,
                    )
                val reportedOnly =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        reports =
                            listOf(
                                report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 7)),
                                report(ReporterType.GRYPE, at = LocalDate.of(2026, 4, 1)),
                            ),
                        verdict = affected,
                    )

                releaseStatuses(analyzed, file).single().since shouldBe LocalDate.of(2026, 4, 6)
                releaseStatuses(reportedOnly, file).single().since shouldBe LocalDate.of(2026, 4, 1)
            }

            test("an open investigation is dated by the first report") {
                val open =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 1))),
                        analyzedAt = LocalDate.of(2026, 4, 6),
                    )

                releaseStatuses(open, file).single().since shouldBe LocalDate.of(2026, 4, 1)
            }

            test("a fix is dated by the resolution date, else the fix release's publication date") {
                val dated =
                    affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0", fixedAt = LocalDate.of(2026, 4, 22))
                val published = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0")
                val undated = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.3.0")

                releaseStatuses(dated, file).last().since shouldBe LocalDate.of(2026, 4, 22)
                releaseStatuses(published, file).last().since shouldBe LocalDate.of(2026, 4, 20)
                releaseStatuses(undated, file).last().since shouldBe null
            }

            test("an undated entry falls back to the day each release was published") {
                val entry = affectedIn(releases = listOf("1.0.0"))

                val statuses = releaseStatuses(entry, file)

                // 1.2.0 is the only release the fixture publishes; the others stay undated.
                statuses.associate { it.release.value to it.since } shouldBe
                    mapOf(
                        "1.0.0" to null,
                        "1.1.0" to null,
                        "1.2.0" to LocalDate.of(2026, 4, 20),
                        "1.3.0" to null,
                    )
            }
        }

        context("status text") {

            test("each status carries the entry's analysis in the field that status owns") {
                val affectedEntry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        verdict = affected,
                        analysis = "the parser is reachable",
                    )
                val notAffectedEntry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        verdict = Verdict.NotAffected(dev.vulnlog.lib.model.VexJustification.COMPONENT_NOT_PRESENT),
                        analysis = "the component is not shipped",
                    )
                val openEntry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        analysis = "waiting on the upstream advisory",
                    )

                releaseStatuses(affectedEntry, file)
                    .single()
                    .status
                    .shouldBeInstanceOf<VexStatus.Affected>()
                    .statusNotes shouldBe "the parser is reachable"
                releaseStatuses(notAffectedEntry, file)
                    .single()
                    .status
                    .shouldBeInstanceOf<VexStatus.NotAffected>()
                    .impactStatement shouldBe "the component is not shipped"
                releaseStatuses(openEntry, file).single().status shouldBe
                    VexStatus.UnderInvestigation("waiting on the upstream advisory")
            }

            test("a fixed status carries no analysis text") {
                val entry =
                    affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0").copy(analysis = "the parser is reachable")

                releaseStatuses(entry, file).last().status shouldBe VexStatus.Fixed
            }

            test("a blank analysis is no analysis") {
                val entry =
                    vulnerability(id = cve("CVE-2026-1234"), releases = listOf(release("1.3.0")), analysis = "  ")

                releaseStatuses(entry, file).single().status shouldBe VexStatus.UnderInvestigation(null)
            }
        }

        context("vexActionStatement") {

            test("points at the fix release and appends the resolution note") {
                val entry = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.0.1", note = "Bumped log4j to 2.17.1.")

                val action = vexActionStatement(entry)

                action shouldBe "Update to release 1.0.1. Bumped log4j to 2.17.1."
            }

            test("points at the fix release alone when no note is recorded") {
                val entry =
                    affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WILL_FIX, fixedIn = "1.0.1")

                val action = vexActionStatement(entry)

                action shouldBe "Update to release 1.0.1."
            }

            test("states that no remediation exists when neither intent nor fix is recorded") {
                val entry = vulnerability(id = cve("CVE-2026-1234"), verdict = affected)

                val action = vexActionStatement(entry)

                action shouldBe "No remediation is available yet."
            }

            test("states that a fix is planned for 'will fix' without a resolution") {
                val entry = affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WILL_FIX)

                val action = vexActionStatement(entry)

                action shouldBe "A fix is planned but not yet available."
            }

            test("states the accepted risk for 'wont fix' without a resolution") {
                val entry = affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WONT_FIX)

                val action = vexActionStatement(entry)

                action shouldBe "The risk is accepted. No fix is planned."
            }

            test("keeps the accepted risk and never appends the note when 'wont fix' has a resolution") {
                val entry =
                    affectedIn(
                        releases = listOf("1.0.0"),
                        disposition = Disposition.WONT_FIX,
                        fixedIn = "1.0.1",
                        note = "Bumped log4j to 2.17.1.",
                    )

                val action = vexActionStatement(entry)

                action shouldBe "The risk is accepted for this release. A fix ships with release 1.0.1."
            }
        }
    })
