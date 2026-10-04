// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private val affected = Verdict.Affected(Severity.HIGH)

/** Only 1.2.0 is published, so a status without a date of its own falls back to it there and nowhere else. */
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

            test("without a resolution the earliest listed release opens a range that stays affected") {
                val entry = affectedIn(releases = listOf("1.2.0", "1.0.0"))

                val statuses = releaseStatuses(entry, file)

                statuses.map { it.release.value } shouldContainExactly listOf("1.0.0", "1.1.0", "1.2.0", "1.3.0")
                statuses.forEach { it.status.shouldBeInstanceOf<VexStatus.Affected>() }
            }

            test("a fix declared before the first listed release wins from the fix on, as the file states it") {
                val entry = affectedIn(releases = listOf("1.2.0"), fixedIn = "1.1.0")

                val statuses = releaseStatuses(entry, file)

                statuses.map { it.release.value } shouldContainExactly listOf("1.1.0", "1.2.0", "1.3.0")
                statuses.all { it.status == VexStatus.Fixed } shouldBe true
            }

            test("an entry listing no release applies nowhere") {
                val entry = vulnerability(id = cve("CVE-2026-1234"), verdict = affected)

                val statuses = releaseStatuses(entry, file)

                statuses.shouldBeEmpty()
            }
        }

        context("releaseStatuses dates") {

            test("a verdict is dated by the analysis date, else the earliest report") {
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

                val dates = listOf(analyzed, reportedOnly).map { releaseStatuses(it, file).single().since }

                dates shouldContainExactly listOf(LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 1))
            }

            test("an open investigation is dated by its first report, since there is no verdict to date") {
                val open =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 1))),
                        analyzedAt = LocalDate.of(2026, 4, 6),
                    )

                val status = releaseStatuses(open, file).single()

                status.since shouldBe LocalDate.of(2026, 4, 1)
            }

            test("a fix is dated by the resolution date, else the fix release's publication date") {
                val dated =
                    affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0", fixedAt = LocalDate.of(2026, 4, 22))
                val published = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0")
                val undated = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.3.0")

                val dates = listOf(dated, published, undated).map { releaseStatuses(it, file).last().since }

                dates shouldContainExactly listOf(LocalDate.of(2026, 4, 22), LocalDate.of(2026, 4, 20), null)
            }

            test("an undated entry falls back to the day each release was published") {
                val entry = affectedIn(releases = listOf("1.0.0"))

                val statuses = releaseStatuses(entry, file)

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
                        verdict = Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT),
                        analysis = "the component is not shipped",
                    )
                val openEntry =
                    vulnerability(
                        id = cve("CVE-2026-1234"),
                        releases = listOf(release("1.3.0")),
                        analysis = "waiting on the upstream advisory",
                    )

                val statuses =
                    listOf(affectedEntry, notAffectedEntry, openEntry).map { releaseStatuses(it, file).single().status }

                statuses shouldContainExactly
                    listOf(
                        VexStatus.Affected(Remediation.NoneAvailable, "the parser is reachable"),
                        VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT, "the component is not shipped"),
                        VexStatus.UnderInvestigation("waiting on the upstream advisory"),
                    )
            }

            test("a fixed status carries no analysis text, because it describes the state before the fix") {
                val entry =
                    affectedIn(releases = listOf("1.0.0"), fixedIn = "1.2.0").copy(analysis = "the parser is reachable")

                val status = releaseStatuses(entry, file).last().status

                status shouldBe VexStatus.Fixed
            }

            test("a blank analysis is no analysis") {
                val entry =
                    vulnerability(id = cve("CVE-2026-1234"), releases = listOf(release("1.3.0")), analysis = "  ")

                val status = releaseStatuses(entry, file).single().status

                status shouldBe VexStatus.UnderInvestigation(null)
            }
        }

        context("vexStatusKind") {

            test("names the kind of every status, and the kinds are declared in sort order") {
                val statuses =
                    listOf(
                        VexStatus.Affected(Remediation.NoneAvailable),
                        VexStatus.Fixed,
                        VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT),
                        VexStatus.UnderInvestigation(),
                    )

                val kinds = statuses.map(::vexStatusKind)

                kinds shouldContainExactly VexStatusKind.entries
            }
        }

        test(
            "filterReleasePurlsMatchingVulnerabilityEntryTags keeps the release purls sharing a tag, never an untagged one",
        ) {
            val release =
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("sca", "container")),
                            mavenPurlEntry("pkg:maven/com.acme/kit@1.0.0", tags = listOf("design-kit")),
                            mavenPurlEntry("pkg:maven/com.acme/untagged@1.0.0"),
                        ),
                )
            val entries =
                listOf(listOf("container"), listOf("design-kit", "build"), listOf("build"), emptyList())
                    .map { tags -> affectedIn(listOf("1.0.0")).copy(tags = tags.map(::tag)) }

            val products =
                entries.map { entry ->
                    filterReleasePurlsMatchingVulnerabilityEntryTags(release, entry).map {
                        it.purl.value
                    }
                }

            products shouldContainExactly
                listOf(
                    listOf("pkg:maven/com.acme/app@1.0.0"),
                    listOf("pkg:maven/com.acme/kit@1.0.0"),
                    emptyList(),
                    emptyList(),
                )
        }

        test("remediationOf follows the disposition and the fix release, never the resolution note") {
            val cases =
                listOf(
                    affectedIn(listOf("1.0.0")) to Remediation.NoneAvailable,
                    affectedIn(listOf("1.0.0"), fixedIn = "1.0.1", note = "Bumped log4j.") to
                        Remediation.UpdateTo(release("1.0.1")),
                    affectedIn(listOf("1.0.0"), Disposition.WILL_FIX) to Remediation.FixPlanned,
                    affectedIn(listOf("1.0.0"), Disposition.WILL_FIX, fixedIn = "1.0.1") to
                        Remediation.UpdateTo(release("1.0.1")),
                    affectedIn(listOf("1.0.0"), Disposition.WONT_FIX) to Remediation.RiskAccepted(fixIn = null),
                    affectedIn(listOf("1.0.0"), Disposition.WONT_FIX, fixedIn = "1.0.1") to
                        Remediation.RiskAccepted(fixIn = release("1.0.1")),
                )

            val remediations = cases.map { (entry, _) -> remediationOf(entry) }

            remediations shouldContainExactly cases.map { (_, expected) -> expected }
        }
    })
