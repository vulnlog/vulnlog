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
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
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

class ApplyFilterTest :
    FunSpec({

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
