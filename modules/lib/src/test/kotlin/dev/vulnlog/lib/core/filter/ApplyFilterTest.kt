// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private val v1 = release("1.0.0")
private val v2 = release("2.0.0")
private val v3 = release("3.0.0")

private val first = cve("CVE-2024-0001")
private val second = cve("CVE-2024-0002")

/** An affected vulnerability whose fix is recorded for [fixedIn]. */
private fun fixedVulnerability(
    reportedFor: List<Release>,
    fixedIn: String,
) = vulnerability(
    first,
    releases = reportedFor,
    verdict = Verdict.Affected(Severity.HIGH),
    resolution = resolution(fixedIn),
)

private fun openVulnerability() = vulnerability(first, releases = listOf(v1), verdict = Verdict.Affected(Severity.HIGH))

private fun acceptedVulnerability() =
    vulnerability(
        second,
        releases = listOf(v1),
        verdict = Verdict.Affected(Severity.LOW, Disposition.WONT_FIX),
    )

private fun willFixVulnerability() =
    vulnerability(
        first,
        releases = listOf(v1),
        verdict = Verdict.Affected(Severity.HIGH, Disposition.WILL_FIX),
    )

private fun notApplicableVulnerability() =
    vulnerability(
        cve("CVE-2024-0003"),
        releases = listOf(v1),
        verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_PRESENT),
    )

class ApplyFilterTest :
    FunSpec({

        context("fixed-in") {

            test("keeps only the vulnerabilities whose fix shipped in that release") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                fixedVulnerability(listOf(v1), "2.0.0"),
                                vulnerability(second, releases = listOf(v1), resolution = resolution("3.0.0")),
                            ),
                    )
                val filter = ResolvedFilter(fixedIn = v2)

                val result = file.applyFilter(filter)

                result.vulnerabilities.single().id shouldBe first
            }

            test("drops a vulnerability that records no resolution") {
                val file = vulnlogFile(vulnerabilities = listOf(openVulnerability()))
                val filter = ResolvedFilter(fixedIn = v2)

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }

            test("keeps every vulnerability when no release is requested") {
                val fixed = fixedVulnerability(listOf(v1), "2.0.0")
                val file = vulnlogFile(vulnerabilities = listOf(openVulnerability(), fixed))
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }
        }

        context("states") {

            test("keeps only the vulnerabilities in the requested state") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), acceptedVulnerability()))
                val filter = ResolvedFilter(states = setOf(WorkState.OPEN))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }

            test("several states select their union") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(openVulnerability(), acceptedVulnerability(), notApplicableVulnerability()),
                    )
                val filter = ResolvedFilter(states = setOf(WorkState.OPEN, WorkState.ACCEPTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities.map { it.id } shouldBe listOf(first, second)
            }

            test("an empty state set leaves every vulnerability in place") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), acceptedVulnerability()))
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }

            test("a state no vulnerability has selects nothing") {
                val file = vulnlogFile(vulnerabilities = listOf(openVulnerability()))
                val filter = ResolvedFilter(states = setOf(WorkState.UNDER_INVESTIGATION))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }

            test("a fix outside the release window does not count as resolved") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "3.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1, v2), states = setOf(WorkState.RESOLVED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }

            test("a fix outside the release window still reads as open") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "3.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1, v2), states = setOf(WorkState.OPEN))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }

            test("a fix inside the release window counts as resolved") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "2.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1, v2), states = setOf(WorkState.RESOLVED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
            }
        }

        context("verdicts") {

            test("keeps only the vulnerabilities with the requested verdict") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), notApplicableVulnerability()))
                val filter = ResolvedFilter(verdicts = setOf(VerdictKind.AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }

            test("several verdicts select their union") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(openVulnerability(), acceptedVulnerability(), notApplicableVulnerability()),
                    )
                val filter =
                    ResolvedFilter(verdicts = setOf(VerdictKind.AFFECTED, VerdictKind.NOT_AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 3
            }

            test("an empty verdict set leaves every vulnerability in place") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), notApplicableVulnerability()))
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }

            test("the untriaged verdict selects the entries without one") {
                val untriaged = vulnerability(cve("CVE-2024-0004"), releases = listOf(v1))
                val file = vulnlogFile(vulnerabilities = listOf(openVulnerability(), untriaged))
                val filter = ResolvedFilter(verdicts = setOf(VerdictKind.UNDER_INVESTIGATION))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe cve("CVE-2024-0004")
            }

            test("an affected verdict spans both the open and the accepted state") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), acceptedVulnerability()))
                val filter = ResolvedFilter(verdicts = setOf(VerdictKind.AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities.map { it.id } shouldBe listOf(first, second)
            }

            test("a verdict ignores the release window that changes the state") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "3.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1, v2), verdicts = setOf(VerdictKind.AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
            }

            test("a verdict and a state narrow each other") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), acceptedVulnerability()))
                val filter =
                    ResolvedFilter(states = setOf(WorkState.ACCEPTED), verdicts = setOf(VerdictKind.AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe second
            }

            test("a verdict and a state that cannot co-occur select nothing") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), notApplicableVulnerability()))
                val filter =
                    ResolvedFilter(states = setOf(WorkState.OPEN), verdicts = setOf(VerdictKind.NOT_AFFECTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }
        }

        context("dispositions") {

            test("keeps only the vulnerabilities with the requested intent") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(willFixVulnerability(), acceptedVulnerability()))
                val filter = ResolvedFilter(dispositions = setOf(Disposition.WONT_FIX))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe second
            }

            test("both dispositions select their union") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(willFixVulnerability(), acceptedVulnerability()))
                val filter =
                    ResolvedFilter(dispositions = setOf(Disposition.WILL_FIX, Disposition.WONT_FIX))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }

            test("an empty disposition set leaves every vulnerability in place") {
                val file =
                    vulnlogFile(vulnerabilities = listOf(openVulnerability(), acceptedVulnerability()))
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }

            test("an affected entry with no stated intent is never selected") {
                val file = vulnlogFile(vulnerabilities = listOf(openVulnerability()))
                val filter =
                    ResolvedFilter(dispositions = setOf(Disposition.WILL_FIX, Disposition.WONT_FIX))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }

            test("a not affected entry is never selected") {
                val file = vulnlogFile(vulnerabilities = listOf(notApplicableVulnerability()))
                val filter =
                    ResolvedFilter(dispositions = setOf(Disposition.WILL_FIX, Disposition.WONT_FIX))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 0
            }

            test("a wont fix entry keeps its intent once the fix ships") {
                val vuln =
                    vulnerability(
                        second,
                        releases = listOf(v1),
                        verdict = Verdict.Affected(Severity.LOW, Disposition.WONT_FIX),
                        resolution = resolution("2.0.0"),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(vuln))
                val filter =
                    ResolvedFilter(
                        releases = setOf(v1, v2),
                        states = setOf(WorkState.RESOLVED),
                        dispositions = setOf(Disposition.WONT_FIX),
                    )

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
            }
        }

        context("release window") {

            test("keeps a vulnerability reported for a release inside the window") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(first, releases = listOf(v1)),
                                vulnerability(second, releases = listOf(v3)),
                            ),
                    )
                val filter = ResolvedFilter(releases = setOf(v1, v2))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }

            test("keeps every vulnerability when no release filter is active") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(first, releases = listOf(v1)),
                                vulnerability(second, releases = listOf(v3)),
                            ),
                    )
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 2
            }
        }

        context("resolution scoping") {

            test("keeps a resolution that shipped inside the window") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1, v2), fixedIn = "2.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1, v2))

                val result = file.applyFilter(filter)

                result.vulnerabilities
                    .first()
                    .resolution
                    .shouldNotBeNull()
            }

            test("drops a resolution that has not shipped inside the window") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "2.0.0")))
                val filter = ResolvedFilter(releases = setOf(v1))

                val result = file.applyFilter(filter)

                result.vulnerabilities
                    .first()
                    .resolution
                    .shouldBeNull()
            }

            test("keeps every resolution when no release filter is active") {
                val file = vulnlogFile(vulnerabilities = listOf(fixedVulnerability(listOf(v1), fixedIn = "2.0.0")))
                val filter = ResolvedFilter()

                val result = file.applyFilter(filter)

                result.vulnerabilities
                    .first()
                    .resolution
                    .shouldNotBeNull()
            }
        }

        context("tags") {

            test("keeps a vulnerability carrying one of the filtered tags") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(first, tags = listOf(tag("internal"))),
                                vulnerability(second, tags = listOf(tag("public"))),
                            ),
                    )
                val filter = ResolvedFilter(tags = setOf(tag("internal")))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }
        }

        context("reporter") {

            test("keeps a vulnerability the filtered reporter reported") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(first, reports = listOf(report(ReporterType.TRIVY))),
                                vulnerability(second, reports = listOf(report(ReporterType.SNYK))),
                            ),
                    )
                val filter = ResolvedFilter(reporter = ReporterType.TRIVY)

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }
        }

        context("combined dimensions") {

            test("a vulnerability must match every active dimension") {
                val matching =
                    vulnerability(
                        first,
                        releases = listOf(v1),
                        reports = listOf(report(ReporterType.TRIVY)),
                        tags = listOf(tag("internal")),
                    )
                val wrongTag =
                    vulnerability(
                        second,
                        releases = listOf(v1),
                        reports = listOf(report(ReporterType.TRIVY)),
                        tags = listOf(tag("public")),
                    )
                val file = vulnlogFile(vulnerabilities = listOf(matching, wrongTag))
                val filter = ResolvedFilter(ReporterType.TRIVY, setOf(v1), setOf(tag("internal")))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe first
            }

            test("a state narrows a set the other dimensions already matched") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                openVulnerability().copy(reports = listOf(report(ReporterType.TRIVY))),
                                acceptedVulnerability().copy(reports = listOf(report(ReporterType.TRIVY))),
                            ),
                    )
                val filter = ResolvedFilter(reporter = ReporterType.TRIVY, states = setOf(WorkState.ACCEPTED))

                val result = file.applyFilter(filter)

                result.vulnerabilities shouldHaveSize 1
                result.vulnerabilities.first().id shouldBe second
            }

            test("leaves everything but the vulnerabilities untouched") {
                val file = vulnlogFile(vulnerabilities = listOf(vulnerability(first)))
                val filter = ResolvedFilter(reporter = ReporterType.TRIVY)

                val result = file.applyFilter(filter)

                result shouldBe file.copy(vulnerabilities = emptyList())
            }
        }

        context("scopeResolution") {

            test("keeps a resolution whose release is inside the window") {
                val vuln = fixedVulnerability(listOf(v1), fixedIn = "1.0.0")

                val scoped = scopeResolution(vuln, setOf(v1))

                scoped shouldBe vuln
            }

            test("drops a resolution whose release is outside the window") {
                val vuln = fixedVulnerability(listOf(v1), fixedIn = "2.0.0")

                val scoped = scopeResolution(vuln, setOf(v1))

                scoped shouldBe vuln.copy(resolution = null)
            }

            test("keeps a resolution when the window is empty") {
                val vuln = fixedVulnerability(listOf(v1), fixedIn = "2.0.0")

                val scoped = scopeResolution(vuln, emptySet())

                scoped shouldBe vuln
            }

            test("leaves a vulnerability without a resolution alone") {
                val vuln = vulnerability(first, releases = listOf(v1))

                val scoped = scopeResolution(vuln, setOf(v1))

                scoped shouldBe vuln
            }
        }
    })
