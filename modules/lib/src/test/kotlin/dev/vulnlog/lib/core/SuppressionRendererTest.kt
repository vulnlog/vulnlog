// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import dev.vulnlog.lib.result.SuppressionExclusion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SuppressionRendererTest :
    FunSpec({

        context("renderSuppressionWritten") {

            test("renders a trivy output with a single entry") {
                val output =
                    SuppressionOutput.TrivySuppression(
                        entries =
                            setOf(
                                SuppressionVuln.TrivySuppressionEntry(
                                    id = VulnId.Cve("CVE-2026-1234"),
                                    reason = "not reachable",
                                ),
                            ),
                    )

                renderSuppressionWritten("/out/.trivyignore.yaml", output) shouldBe
                    "wrote /out/.trivyignore.yaml: trivy format, 1 entry"
            }

            test("pluralizes the entry count") {
                val output =
                    SuppressionOutput.GenericSuppression(
                        entries =
                            setOf(
                                SuppressionVuln.GenericSuppressionEntry(
                                    id = VulnId.Cve("CVE-2026-1234"),
                                    reason = "not reachable",
                                ),
                                SuppressionVuln.GenericSuppressionEntry(
                                    id = VulnId.Ghsa("GHSA-aaaa-bbbb-cccc"),
                                    reason = "not reachable",
                                ),
                            ),
                    )

                renderSuppressionWritten("trivy.generic.json", output) shouldBe
                    "wrote trivy.generic.json: generic format, 2 entries"
            }

            test("renders an empty snyk output") {
                val output = SuppressionOutput.SnykSuppression(entries = emptySet())

                renderSuppressionWritten("<stdout>", output) shouldBe "wrote <stdout>: snyk format, 0 entries"
            }

            test("renders the cargo-audit format name") {
                val entry = SuppressionVuln.CargoAuditSuppressionEntry(id = VulnId.RustSec("RUSTSEC-2026-0001"))
                val output = SuppressionOutput.CargoAuditSuppression(entries = setOf(entry))

                renderSuppressionWritten("audit.toml", output) shouldBe
                    "wrote audit.toml: cargo-audit format, 1 entry"
            }
        }

        context("renderSuppressionInclusions") {

            test("renders one sorted line per included entry, with the expiry when present") {
                val included =
                    mapOf(
                        ReporterType.TRIVY to
                            listOf(
                                SuppressedVulnerability(
                                    id = VulnId.Cve("CVE-2026-1234"),
                                    releases = emptyList(),
                                    reporter = ReporterType.TRIVY,
                                    expiresAt = LocalDate.of(2026, 9, 1),
                                    analysis = "not reachable",
                                ),
                            ),
                        ReporterType.SNYK to
                            listOf(
                                SuppressedVulnerability(
                                    id = VulnId.Cve("CVE-2026-0002"),
                                    releases = emptyList(),
                                    reporter = ReporterType.SNYK,
                                    expiresAt = null,
                                    analysis = "not reachable",
                                ),
                            ),
                    )

                renderSuppressionInclusions(included) shouldBe
                    listOf(
                        "included CVE-2026-0002 for reporter snyk",
                        "included CVE-2026-1234 for reporter trivy (expires 2026-09-01)",
                    )
            }

            test("renders nothing when nothing was included") {
                renderSuppressionInclusions(emptyMap()) shouldBe emptyList()
            }
        }

        context("renderSuppressionExclusion") {

            test("names the file and the required id type for an unsupported id type") {
                val exclusion =
                    SuppressionExclusion.UnsupportedIdType(
                        id = VulnId.Cve("CVE-2026-1234"),
                        fileName = ".snyk",
                        format = SuppressionFormat.NativeFormat.Snyk,
                    )

                renderSuppressionExclusion(exclusion) shouldBe
                    "skipped CVE-2026-1234 for .snyk: the snyk format requires SNYK ids"
            }

            test("lists all id types a format accepts") {
                val exclusion =
                    SuppressionExclusion.UnsupportedIdType(
                        id = VulnId.RustSec("RUSTSEC-2026-0001"),
                        fileName = ".trivyignore.yaml",
                        format = SuppressionFormat.NativeFormat.Trivy,
                    )

                renderSuppressionExclusion(exclusion) shouldBe
                    "skipped RUSTSEC-2026-0001 for .trivyignore.yaml: the trivy format requires CVE or GHSA ids"
            }

            test("names the reporter for an unsupported reporter") {
                val exclusion =
                    SuppressionExclusion.UnsupportedReporter(VulnId.Cve("CVE-2026-1234"), ReporterType.OTHER)

                renderSuppressionExclusion(exclusion) shouldBe
                    "skipped CVE-2026-1234 for reporter other: no suppression format available"
            }

            test("explains a resolved vulnerability") {
                val exclusion = SuppressionExclusion.ResolvedVulnerability(VulnId.Cve("CVE-2026-1234"))

                renderSuppressionExclusion(exclusion) shouldBe
                    "skipped CVE-2026-1234: resolved vulnerabilities are not suppressed"
            }

            test("names the expiry date of an expired suppression") {
                val exclusion =
                    SuppressionExclusion.ExpiredSuppression(
                        VulnId.Cve("CVE-2026-1234"),
                        ReporterType.TRIVY,
                        LocalDate.of(2026, 1, 31),
                    )

                renderSuppressionExclusion(exclusion) shouldBe
                    "skipped CVE-2026-1234 for reporter trivy: suppression expired on 2026-01-31"
            }
        }
    })
