// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.model.finding.ValidationFinding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

private fun applyV1Rules(file: VulnlogFile): List<ValidationFinding> = v1DomainRules.flatMap { rule -> rule(file) }

class DomainRulesTest :
    FunSpec({

        test("an empty file breaks no rule") {
            val file = vulnlogFile()

            val findings = applyV1Rules(file)

            findings.shouldBeEmpty()
        }

        context("duplicate release IDs") {

            test("unique release IDs produce no finding") {
                val file = vulnlogFile(releases = listOf(releaseEntry("v1.0"), releaseEntry("v2.0")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_RELEASE_ID }

                findings.shouldBeEmpty()
            }

            test("a repeated release ID is an error") {
                val file = vulnlogFile(releases = listOf(releaseEntry("v1.0"), releaseEntry("v1.0")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_RELEASE_ID }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "releases[v1.0]"
                    message shouldBe "Duplicate release ID 'v1.0'."
                }
            }
        }

        context("duplicate tag IDs") {

            test("unique tag IDs produce no finding") {
                val file = vulnlogFile(tags = listOf(tagEntry("backend"), tagEntry("frontend")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_TAG_ID }

                findings.shouldBeEmpty()
            }

            test("a repeated tag ID is an error") {
                val file = vulnlogFile(tags = listOf(tagEntry("backend"), tagEntry("backend")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_TAG_ID }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "tags[backend]"
                    message shouldBe "Duplicate tag ID 'backend'."
                }
            }
        }

        context("duplicate vulnerability IDs") {

            test("unique vulnerability IDs produce no finding") {
                val file =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1")), vulnerability(cve("CVE-2021-2"))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_VULNERABILITY_ID }

                findings.shouldBeEmpty()
            }

            test("a repeated vulnerability ID is an error") {
                val vuln = vulnerability(cve("CVE-2021-1"))
                val file = vulnlogFile(vulnerabilities = listOf(vuln, vuln))

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_VULNERABILITY_ID }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "vulnerabilities[CVE-2021-1]"
                }
            }

            test("an alias that is also a primary ID is an error") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(cve("CVE-2021-1")),
                                vulnerability(ghsa("GHSA-aaaa-bbbb-cccc"), aliases = listOf(cve("CVE-2021-1"))),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_VULNERABILITY_ID }

                findings
                    .single()
                    .path shouldBe "vulnerabilities[GHSA-AAAA-BBBB-CCCC].aliases[CVE-2021-1]"
            }

            test("an alias that is unique to its vulnerability produces no finding") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(vulnerability(cve("CVE-2021-1"), aliases = listOf(ghsa("GHSA-aaaa-bbbb-cccc")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_VULNERABILITY_ID }

                findings.shouldBeEmpty()
            }

            test("an alias shared by two vulnerabilities is an error on each of them") {
                val sharedAlias = ghsa("GHSA-aaaa-bbbb-cccc")
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(cve("CVE-2021-1"), aliases = listOf(sharedAlias)),
                                vulnerability(cve("CVE-2021-2"), aliases = listOf(sharedAlias)),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DUPLICATE_VULNERABILITY_ID }

                findings shouldHaveSize 2
                findings.forEach { it.message shouldContain "CVE-2021-1, CVE-2021-2" }
            }
        }

        context("dangling release references") {

            test("a reference to a defined release produces no finding") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), releases = listOf(release("v1.0")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_RELEASE_REFERENCE }

                findings.shouldBeEmpty()
            }

            test("a reference to an undefined release is an error naming the defined ones") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0"), releaseEntry("v1.1")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), releases = listOf(release("v9.9")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_RELEASE_REFERENCE }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "vulnerabilities[CVE-2021-1].releases"
                    message shouldContain "v9.9"
                    message shouldContain "v1.0"
                    message shouldContain "v1.1"
                }
            }

            test("a resolution pointing at an undefined release is an error") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), resolution = resolution("v2.0"))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_RELEASE_REFERENCE }

                with(findings.single()) {
                    path shouldBe "vulnerabilities[CVE-2021-1].resolution"
                    message shouldContain "v2.0"
                }
            }
        }

        context("dangling tag references") {

            test("a defined tag on a vulnerability produces no finding") {
                val file =
                    vulnlogFile(
                        tags = listOf(tagEntry("backend")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), tags = listOf(tag("backend")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_TAG_REFERENCE }

                findings.shouldBeEmpty()
            }

            test("an undefined tag on a vulnerability is an error") {
                val file =
                    vulnlogFile(
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), tags = listOf(tag("unknown")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_TAG_REFERENCE }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "vulnerabilities[CVE-2021-1].tags[unknown]"
                }
            }

            test("a defined tag on a release PURL produces no finding") {
                val file =
                    vulnlogFile(
                        tags = listOf(tagEntry("backend")),
                        releases =
                            listOf(
                                releaseEntry(
                                    "v1.0",
                                    purls =
                                        listOf(
                                            mavenPurlEntry("pkg:maven/acme/widget@1.0", tags = listOf("backend")),
                                        ),
                                ),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_TAG_REFERENCE }

                findings.shouldBeEmpty()
            }

            test("an undefined tag on a release PURL is an error") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry(
                                    "v1.0",
                                    purls =
                                        listOf(
                                            mavenPurlEntry("pkg:maven/acme/widget@1.0", tags = listOf("unknown")),
                                        ),
                                ),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.DANGLING_TAG_REFERENCE }

                findings
                    .single()
                    .path shouldBe "releases[v1.0].purls[pkg:maven/acme/widget@1.0].tags[unknown]"
            }
        }

        context("analyzed date against the earliest report date") {

            test("an analysis after the report produces no finding") {
                val file = fileAnalyzedAt(LocalDate.of(2021, 6, 1), reportedAt = LocalDate.of(2021, 1, 1))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                findings.shouldBeEmpty()
            }

            test("an analysis on the day of the report produces no finding") {
                val sameDay = LocalDate.of(2021, 1, 1)
                val file = fileAnalyzedAt(sameDay, reportedAt = sameDay)

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                findings.shouldBeEmpty()
            }

            test("an analysis before the report is a warning") {
                val file = fileAnalyzedAt(LocalDate.of(2021, 1, 1), reportedAt = LocalDate.of(2021, 6, 1))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.WARNING
                    path shouldBe "vulnerabilities[CVE-2021-1].analyzed_at"
                }
            }

            test("a missing analysis date produces no finding") {
                val file = fileAnalyzedAt(null, reportedAt = LocalDate.of(2021, 1, 1))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                findings.shouldBeEmpty()
            }

            test("a report without a date does not count as the earliest") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    cve("CVE-2021-1"),
                                    reports =
                                        listOf(
                                            report(ReporterType.GRYPE, at = null),
                                            report(ReporterType.SNYK, at = LocalDate.of(2021, 6, 1)),
                                        ),
                                    analyzedAt = LocalDate.of(2021, 1, 1),
                                ),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                findings shouldHaveSize 1
            }

            test("reports without any date produce no finding") {
                val file = fileAnalyzedAt(LocalDate.of(2021, 1, 1), reportedAt = null)

                val findings = applyV1Rules(file).filter { it.rule == Rule.ANALYZED_BEFORE_REPORTED }

                findings.shouldBeEmpty()
            }
        }

        context("reporter information") {

            test("the generic reporter with a source produces no finding") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    cve("CVE-2021-1"),
                                    reports = listOf(report(ReporterType.OTHER, source = "https://example.com")),
                                ),
                            ),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.MISSING_REPORTER_INFORMATION }

                findings.shouldBeEmpty()
            }

            test("the generic reporter without a source is an error") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(vulnerability(cve("CVE-2021-1"), reports = listOf(report(ReporterType.OTHER)))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.MISSING_REPORTER_INFORMATION }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    path shouldBe "vulnerabilities[CVE-2021-1]"
                }
            }

            test("a named reporter needs no source") {
                val file =
                    vulnlogFile(
                        vulnerabilities =
                            listOf(vulnerability(cve("CVE-2021-1"), reports = listOf(report(ReporterType.GRYPE)))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.MISSING_REPORTER_INFORMATION }

                findings.shouldBeEmpty()
            }
        }

        context("unreferenced releases") {

            test("a release used by a vulnerability produces no finding") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), releases = listOf(release("v1.0")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_RELEASE_ID }

                findings.shouldBeEmpty()
            }

            test("a release nothing points at is informational") {
                val file = vulnlogFile(releases = listOf(releaseEntry("v1.0")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_RELEASE_ID }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.INFO
                    path shouldBe "releases[v1.0]"
                }
            }

            test("only the unused releases are reported") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0"), releaseEntry("v2.0")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), releases = listOf(release("v1.0")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_RELEASE_ID }

                findings
                    .single()
                    .path shouldBe "releases[v2.0]"
            }
        }

        context("unreferenced tags") {

            test("a tag used by a vulnerability produces no finding") {
                val file =
                    vulnlogFile(
                        tags = listOf(tagEntry("backend")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), tags = listOf(tag("backend")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_TAG_ID }

                findings.shouldBeEmpty()
            }

            test("a tag used by a release PURL produces no finding") {
                val file =
                    vulnlogFile(
                        tags = listOf(tagEntry("backend")),
                        releases =
                            listOf(
                                releaseEntry(
                                    "v1.0",
                                    purls =
                                        listOf(
                                            mavenPurlEntry("pkg:maven/acme/widget@1.0", tags = listOf("backend")),
                                        ),
                                ),
                            ),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), releases = listOf(release("v1.0")))),
                    )

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_TAG_ID }

                findings.shouldBeEmpty()
            }

            test("a tag nothing points at is informational") {
                val file = vulnlogFile(tags = listOf(tagEntry("backend")))

                val findings = applyV1Rules(file).filter { it.rule == Rule.UNREFERENCED_TAG_ID }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.INFO
                    path shouldBe "tags[backend]"
                }
            }
        }

        context("accepted critical risk") {

            test("a critical vulnerability marked wont fix is informational") {
                val file = fileWithVerdict(Verdict.Affected(Severity.CRITICAL, Disposition.WONT_FIX))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ACCEPTED_CRITICAL_RISK }

                with(findings.single()) {
                    severity shouldBe FindingSeverity.INFO
                    path shouldBe "vulnerabilities[CVE-2021-1].disposition"
                }
            }

            test("a non-critical vulnerability marked wont fix produces no finding") {
                val file = fileWithVerdict(Verdict.Affected(Severity.HIGH, Disposition.WONT_FIX))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ACCEPTED_CRITICAL_RISK }

                findings.shouldBeEmpty()
            }

            test("a critical vulnerability that is being fixed produces no finding") {
                val file = fileWithVerdict(Verdict.Affected(Severity.CRITICAL))

                val findings = applyV1Rules(file).filter { it.rule == Rule.ACCEPTED_CRITICAL_RISK }

                findings.shouldBeEmpty()
            }
        }
    })

private fun fileAnalyzedAt(
    analyzedAt: LocalDate?,
    reportedAt: LocalDate?,
): VulnlogFile =
    vulnlogFile(
        vulnerabilities =
            listOf(
                vulnerability(
                    cve("CVE-2021-1"),
                    reports = listOf(report(ReporterType.GRYPE, at = reportedAt)),
                    analyzedAt = analyzedAt,
                ),
            ),
    )

private fun fileWithVerdict(verdict: Verdict): VulnlogFile =
    vulnlogFile(vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"), verdict = verdict)))
