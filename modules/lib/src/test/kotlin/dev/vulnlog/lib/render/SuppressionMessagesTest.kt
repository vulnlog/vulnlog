// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.SuppressionFile
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressedVulnerability
import dev.vulnlog.lib.model.suppression.SuppressionCollection
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val CVE = VulnId.Cve("CVE-2026-1234")

private fun suppressed(
    id: VulnId,
    reporter: ReporterType,
    expiresAt: LocalDate? = null,
) = SuppressedVulnerability(id, emptyList(), reporter, expiresAt, analysis = "not reachable")

private fun generated(
    filter: ResolvedFilter = ResolvedFilter(),
    included: Map<ReporterType, List<SuppressedVulnerability>> = emptyMap(),
    exclusions: List<SuppressionExclusion> = emptyList(),
) = SuppressionOutcome.Generated(filter, SuppressionCollection(included, exclusions), emptyList())

class SuppressionMessagesTest :
    FunSpec({

        context("renderSuppressionMessages") {

            test("reports the filter and the exclusions verbosely, then every inclusion, sorted, at debug") {
                val outcome =
                    generated(
                        filter = ResolvedFilter(reporter = ReporterType.TRIVY),
                        included =
                            mapOf(
                                ReporterType.TRIVY to
                                    listOf(suppressed(CVE, ReporterType.TRIVY, expiresAt = LocalDate.of(2026, 9, 1))),
                                ReporterType.SNYK to listOf(suppressed(VulnId.Cve("CVE-2026-0002"), ReporterType.SNYK)),
                            ),
                        exclusions = listOf(SuppressionExclusion.Resolved(VulnId.Cve("CVE-2026-0003"))),
                    )

                val messages = renderSuppressionMessages(outcome)

                messages shouldContainExactly
                    listOf(
                        Message.Verbose("reporter filter: trivy"),
                        Message.Verbose("skipped CVE-2026-0003: resolved vulnerabilities are not suppressed"),
                        Message.Debug("included CVE-2026-0002 for reporter snyk"),
                        Message.Debug("included CVE-2026-1234 for reporter trivy (expires 2026-09-01)"),
                    )
            }

            test("words why each excluded entry is left out") {
                val outcome =
                    generated(
                        exclusions =
                            listOf(
                                SuppressionExclusion.UnsupportedIdType(CVE, SuppressionFormat.Snyk),
                                SuppressionExclusion.UnsupportedIdType(
                                    VulnId.RustSec("RUSTSEC-2026-0001"),
                                    SuppressionFormat.Trivy,
                                ),
                                SuppressionExclusion.UnsupportedReporter(CVE, ReporterType.OTHER),
                                SuppressionExclusion.Expired(CVE, ReporterType.TRIVY, LocalDate.of(2026, 1, 31)),
                            ),
                    )

                val texts = renderSuppressionMessages(outcome).map { it.text }

                texts shouldContainExactly
                    listOf(
                        "skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids",
                        "skipped RUSTSEC-2026-0001 for .trivyignore.yaml: the trivy format requires CVE or GHSA ids",
                        "skipped CVE-2026-1234 for reporter other: no suppression format available",
                        "skipped CVE-2026-1234 for reporter trivy: suppression expired on 2026-01-31",
                    )
            }

            test("nothing to suppress ends with the unchanged status") {
                val exclusion = SuppressionExclusion.UnsupportedReporter(CVE, ReporterType.OTHER)
                val collection = SuppressionCollection(emptyMap(), listOf(exclusion))
                val outcome = SuppressionOutcome.NothingToSuppress(ResolvedFilter(), collection)

                val messages = renderSuppressionMessages(outcome)

                messages shouldContainExactly
                    listOf(
                        Message.Verbose("skipped CVE-2026-1234 for reporter other: no suppression format available"),
                        Message.Status("Unchanged: no suppression entries applicable"),
                    )
            }

            test("a rejected filter reports nothing besides its failure") {
                val outcome = FilterRejected(listOf(FilterProblem.UnknownReporter("bogus")))

                val messages = renderSuppressionMessages(outcome)

                messages.shouldBeEmpty()
            }
        }

        context("renderSuppressionFailure") {

            test("several reporters for one file names them, sorted, and the options that resolve it") {
                val failed =
                    SuppressionOutcome.SeveralReporters(
                        ResolvedFilter(),
                        SuppressionCollection(emptyMap(), emptyList()),
                        listOf(ReporterType.TRIVY, ReporterType.SNYK),
                    )

                val failures = renderSuppressionFailure(failed, "-o", "--reporter", "--output-dir")

                failures shouldContainExactly
                    listOf(
                        Failure(
                            "-o requires a single reporter, found: snyk, trivy",
                            "use --reporter <name> to pick one, or --output-dir for one file per reporter",
                        ),
                    )
            }

            test("a rejected filter is worded like every filter problem") {
                val problems = listOf(FilterProblem.UnknownReporter("bogus"))

                val failures = renderSuppressionFailure(FilterRejected(problems), "-o", "--reporter", "--output-dir")

                failures shouldBe renderFilterProblems(problems)
            }
        }

        test("renderSuppressionWritten names the target, the format and the entry count") {
            val one = setOf(SuppressionEntry(CVE))
            val two = one + SuppressionEntry(VulnId.Cve("CVE-2026-0002"))
            val files =
                listOf(
                    ".trivyignore.yaml" to SuppressionList(SuppressionFormat.Trivy, one),
                    "grype.generic.json" to SuppressionList(SuppressionFormat.Generic(ReporterType.GRYPE), two),
                    "<stdout>" to SuppressionList(SuppressionFormat.Snyk, emptySet()),
                    "audit.toml" to SuppressionList(SuppressionFormat.CargoAudit, one),
                )

            val messages = files.map { (target, list) -> renderSuppressionWritten(target, SuppressionFile(list, "")) }

            messages shouldContainExactly
                listOf(
                    Message.Verbose("wrote .trivyignore.yaml: trivy format, 1 entry"),
                    Message.Verbose("wrote grype.generic.json: generic format, 2 entries"),
                    Message.Verbose("wrote <stdout>: snyk format, 0 entries"),
                    Message.Verbose("wrote audit.toml: cargo-audit format, 1 entry"),
                )
        }
    })
