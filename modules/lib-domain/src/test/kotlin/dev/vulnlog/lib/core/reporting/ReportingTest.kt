// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val releaseV1 = Release("v1.0")
private val releaseV2 = Release("v2.0")
private val releaseV3 = Release("v3.0")
private val cve1 = VulnId.Cve("CVE-2024-0001")
private val cve2 = VulnId.Cve("CVE-2024-0002")
private val ghsa1 = VulnId.Ghsa("GHSA-1234-5678-abcd")

private fun collected(vulnerability: VulnerabilityEntry): ImpactEntry =
    collectImpactEntries(vulnlogFile(vulnerabilities = listOf(vulnerability))).single()

private fun impactEntry(
    primaryId: VulnId = cve1,
    state: WorkState = WorkState.OPEN,
    ids: Set<VulnId> = setOf(primaryId),
    impact: Impact = Impact.NotAffected("vulnerable code not in execute path"),
    disposition: Disposition? = null,
    analysis: String? = "not affected",
    reportFor: Set<Release> = setOf(releaseV1),
    fixedIn: Set<Release> = emptySet(),
    shortDescription: String? = null,
) = ImpactEntry(
    primaryId = primaryId,
    state = state,
    ids = ids,
    shortDescription = shortDescription,
    impact = impact,
    disposition = disposition,
    analysis = analysis,
    reportFor = reportFor,
    fixedIn = fixedIn,
)

class ReportingTest :
    FunSpec({

        context("collectImpactEntries") {

            test("keeps the primary id apart from its aliases") {
                val vulnerability = vulnerability(id = cve1, aliases = listOf(ghsa1))

                val entry = collected(vulnerability)

                entry.primaryId shouldBe cve1
                entry.ids shouldBe setOf(ghsa1)
            }

            test("takes the fix release from the resolution, and none without one") {
                val fixed = vulnerability(id = cve1, resolution = Resolution(release = releaseV2))
                val unfixed = vulnerability(id = cve2)

                val entries = listOf(fixed, unfixed).map(::collected)

                entries.map { it.fixedIn } shouldContainExactly listOf(setOf(releaseV2), emptySet())
            }

            test("carries the state, the impact and the stated disposition") {
                val verdict = Verdict.Affected(Severity.LOW, Disposition.WONT_FIX)

                val entry = collected(vulnerability(id = cve1, verdict = verdict))

                entry.state shouldBe WorkState.ACCEPTED
                entry.impact shouldBe Impact.Affected(Severity.LOW)
                entry.disposition shouldBe Disposition.WONT_FIX
            }

            test("a not affected entry carries its justification and no disposition") {
                val justification = VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH

                val entry = collected(vulnerability(id = cve1, verdict = Verdict.NotAffected(justification)))

                entry.impact shouldBe Impact.NotAffected(justification.value)
                entry.disposition.shouldBeNull()
            }

            test("leaves the disposition empty when the intent is not stated") {
                val vulnerability = vulnerability(id = cve1, verdict = Verdict.Affected(Severity.LOW))

                val entry = collected(vulnerability)

                entry.disposition.shouldBeNull()
            }
        }

        context("mergeImpactEntries") {

            test("merges the rows of one vulnerability, uniting aliases, releases and fixes") {
                val entries =
                    listOf(
                        impactEntry(ids = setOf(cve1), reportFor = setOf(releaseV1), fixedIn = setOf(releaseV1)),
                        impactEntry(ids = setOf(cve1, ghsa1), reportFor = setOf(releaseV2), fixedIn = setOf(releaseV2)),
                        impactEntry(ids = setOf(cve1), reportFor = setOf(releaseV3), fixedIn = setOf(releaseV3)),
                    )

                val merged = mergeImpactEntries(entries)

                merged shouldContainExactly
                    listOf(
                        impactEntry(
                            ids = setOf(cve1, ghsa1),
                            reportFor = setOf(releaseV1, releaseV2, releaseV3),
                            fixedIn = setOf(releaseV1, releaseV2, releaseV3),
                        ),
                    )
            }

            test("takes the first short description found") {
                val entries = listOf(impactEntry(shortDescription = null), impactEntry(shortDescription = "RCE in lib"))

                val merged = mergeImpactEntries(entries)

                merged.single().shortDescription shouldBe "RCE in lib"
            }

            test("keeps rows apart that disagree on state, impact, disposition or analysis") {
                val disagreeing =
                    listOf(
                        impactEntry(state = WorkState.OPEN) to impactEntry(state = WorkState.RESOLVED),
                        impactEntry(impact = Impact.Affected(Severity.HIGH)) to impactEntry(impact = Impact.Unknown),
                        impactEntry(disposition = Disposition.WILL_FIX) to
                            impactEntry(disposition = Disposition.WONT_FIX),
                        impactEntry(analysis = "not reachable") to impactEntry(analysis = "mitigated by WAF"),
                    )

                val merged = disagreeing.map { (first, second) -> mergeImpactEntries(listOf(first, second)) }

                merged.forEach { it shouldHaveSize 2 }
            }

            test("keeps different vulnerabilities apart") {
                val entries = listOf(impactEntry(primaryId = cve1), impactEntry(primaryId = cve2))

                val merged = mergeImpactEntries(entries)

                merged shouldBe entries
            }
        }

        context("sharedProject") {

            test("returns the project every file declares") {
                val files = listOf(vulnlogFile(), vulnlogFile())

                val project = sharedProject(files)

                project shouldBe vulnlogFile().project
            }

            test("returns none when the files declare different projects") {
                val files = listOf(vulnlogFile(), vulnlogFile(project = Project("other-org", "other", "other-author")))

                val project = sharedProject(files)

                project.shouldBeNull()
            }
        }
    })
