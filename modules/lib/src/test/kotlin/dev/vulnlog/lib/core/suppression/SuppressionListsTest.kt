// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.suppression

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressedVulnerability
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private val CVE = VulnId.Cve("CVE-2024-0001")
private val EXPIRY = LocalDate.of(2026, 12, 31)

private fun suppressed(
    id: VulnId = CVE,
    reporter: ReporterType = ReporterType.TRIVY,
    expiresAt: LocalDate? = null,
    analysis: String = "not reachable",
) = SuppressedVulnerability(
    id = id,
    releases = listOf(Release("1.0.0")),
    reporter = reporter,
    expiresAt = expiresAt,
    analysis = analysis,
)

class SuppressionListsTest :
    FunSpec({

        context("formats") {

            test("trivy, snyk and cargo-audit have a native format, other reporters none") {
                val reporters = ReporterType.entries

                val native = reporters.associateWith(::nativeSuppressionFormat).filterValues { it != null }

                native shouldBe
                    mapOf(
                        ReporterType.CARGO_AUDIT to SuppressionFormat.CargoAudit,
                        ReporterType.SNYK to SuppressionFormat.Snyk,
                        ReporterType.TRIVY to SuppressionFormat.Trivy,
                    )
            }

            test("every reporter but other has the generic format") {
                val reporters = listOf(ReporterType.TRIVY, ReporterType.OTHER)

                val generic = reporters.map(::genericSuppressionFormat)

                generic shouldContainExactly listOf(SuppressionFormat.Generic(ReporterType.TRIVY), null)
            }

            test("each format has the file name its scanner looks for") {
                val formats =
                    listOf(
                        SuppressionFormat.Trivy,
                        SuppressionFormat.Snyk,
                        SuppressionFormat.CargoAudit,
                        SuppressionFormat.Generic(ReporterType.GITHUB_DEPENDABOT),
                    )

                val names = formats.map(::suppressionFileName)

                names shouldContainExactly
                    listOf(".trivyignore.yaml", ".snyk", "audit.toml", "github-dependabot.generic.json")
            }
        }

        context("buildSuppressionLists") {

            test("builds one list per reporter with a format, carrying the expiry and the analysis as reason") {
                val included =
                    mapOf(
                        ReporterType.TRIVY to listOf(suppressed(expiresAt = EXPIRY)),
                        ReporterType.GRYPE to listOf(suppressed(reporter = ReporterType.GRYPE)),
                    )
                val formats =
                    mapOf(
                        ReporterType.TRIVY to SuppressionFormat.Trivy,
                        ReporterType.GRYPE to SuppressionFormat.Generic(ReporterType.GRYPE),
                    )

                val built = buildSuppressionLists(included, formats)

                built.lists shouldContainExactly
                    listOf(
                        SuppressionList(SuppressionFormat.Trivy, setOf(SuppressionEntry(CVE, EXPIRY, "not reachable"))),
                        SuppressionList(
                            SuppressionFormat.Generic(ReporterType.GRYPE),
                            setOf(SuppressionEntry(CVE, null, "not reachable")),
                        ),
                    )
                built.exclusions.shouldBeEmpty()
            }

            test("a reporter with nothing to suppress still gets an empty list") {
                val formats = mapOf(ReporterType.SNYK to SuppressionFormat.Snyk)

                val built = buildSuppressionLists(emptyMap(), formats)

                built.lists shouldContainExactly listOf(SuppressionList(SuppressionFormat.Snyk, emptySet()))
            }

            test("identical suppressions collapse to one entry") {
                val included = mapOf(ReporterType.TRIVY to listOf(suppressed(), suppressed()))

                val built = buildSuppressionLists(included, mapOf(ReporterType.TRIVY to SuppressionFormat.Trivy))

                built.lists.single().entries shouldBe setOf(SuppressionEntry(CVE, null, "not reachable"))
            }

            test("cargo-audit entries keep only the id, so one id is one entry") {
                val rustSec = VulnId.RustSec("RUSTSEC-2026-0001")
                val included =
                    mapOf(
                        ReporterType.CARGO_AUDIT to
                            listOf(
                                suppressed(rustSec, ReporterType.CARGO_AUDIT, analysis = "first"),
                                suppressed(rustSec, ReporterType.CARGO_AUDIT, expiresAt = EXPIRY, analysis = "second"),
                            ),
                    )

                val built =
                    buildSuppressionLists(included, mapOf(ReporterType.CARGO_AUDIT to SuppressionFormat.CargoAudit))

                built.lists.single().entries shouldBe setOf(SuppressionEntry(rustSec))
            }

            test("an id the format cannot hold is left out and reported once") {
                val included = mapOf(ReporterType.SNYK to listOf(suppressed(), suppressed()))

                val built = buildSuppressionLists(included, mapOf(ReporterType.SNYK to SuppressionFormat.Snyk))

                built.lists shouldContainExactly listOf(SuppressionList(SuppressionFormat.Snyk, emptySet()))
                built.exclusions shouldContainExactly
                    listOf(SuppressionExclusion.UnsupportedIdType(CVE, SuppressionFormat.Snyk))
            }

            test("the generic format holds the ids a native format cannot") {
                val included =
                    mapOf(ReporterType.CARGO_AUDIT to listOf(suppressed(reporter = ReporterType.CARGO_AUDIT)))
                val formats = mapOf(ReporterType.CARGO_AUDIT to SuppressionFormat.Generic(ReporterType.CARGO_AUDIT))

                val built = buildSuppressionLists(included, formats)

                built.lists.single().entries shouldBe setOf(SuppressionEntry(CVE, null, "not reachable"))
            }

            test("a reporter without a format gets no list, and its entries are reported") {
                val included = mapOf(ReporterType.OTHER to listOf(suppressed(reporter = ReporterType.OTHER)))

                val built = buildSuppressionLists(included, mapOf(ReporterType.OTHER to null))

                built.lists.shouldBeEmpty()
                built.exclusions shouldContainExactly
                    listOf(SuppressionExclusion.UnsupportedReporter(CVE, ReporterType.OTHER))
            }
        }
    })
