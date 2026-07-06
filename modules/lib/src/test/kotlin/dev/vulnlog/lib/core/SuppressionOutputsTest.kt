// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.result.SuppressionExclusion
import dev.vulnlog.lib.shell.SuppressionFormatRequest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private val releaseV1 = Release("v1.0")

private fun suppressedVuln(
    id: VulnId = VulnId.Cve("CVE-2024-0001"),
    reporter: ReporterType = ReporterType.TRIVY,
    expiresAt: LocalDate? = null,
    analysis: String = "not affected",
) = SuppressedVulnerability(
    id = id,
    releases = listOf(releaseV1),
    reporter = reporter,
    expiresAt = expiresAt,
    analysis = analysis,
)

class SuppressionOutputsTest :
    FunSpec({

        context("buildSuppressionOutputs for Trivy") {

            test("maps trivy suppressions to TrivySuppression output") {
                val entry = suppressedVuln()
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).outputs

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.TrivySuppression>()
            }

            test("filters out non-suppressable reporters") {
                val entry = suppressedVuln(reporter = ReporterType.OTHER)
                val input = mapOf(ReporterType.OTHER to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.OTHER), input).outputs

                result.shouldBeEmpty()
            }

            test("produces correct trivy entries from CVE") {
                val entry = suppressedVuln(id = VulnId.Cve("CVE-2024-1234"), analysis = "false positive")
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).outputs
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries shouldHaveSize 1
                trivy.entries.first().id shouldBe VulnId.Cve("CVE-2024-1234")
                trivy.entries.first().reason shouldBe "false positive"
            }

            test("produces empty TrivySuppression when no suppressions match") {
                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), emptyMap()).outputs

                result shouldHaveSize 1
                val trivy = result.first() as SuppressionOutput.TrivySuppression
                trivy.entries.shouldBeEmpty()
            }

            test("ignores target reporters not in suppressable set") {
                val result = buildSuppressionOutputs(setOf(ReporterType.OTHER, ReporterType.TRIVY), emptyMap()).outputs

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.TrivySuppression>()
            }

            test("produces empty output when no target reporters") {
                val result = buildSuppressionOutputs(emptySet(), emptyMap()).outputs

                result.shouldBeEmpty()
            }

            test("propagates expiresAt to trivy entries") {
                val expiresAt = LocalDate.of(2026, 12, 31)
                val entry = suppressedVuln(expiresAt = expiresAt)
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).outputs
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries.first().expiresAt shouldBe expiresAt
            }

            test("sets expiresAt to null for permanent suppression") {
                val entry = suppressedVuln(expiresAt = null)
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).outputs
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries.first().expiresAt shouldBe null
            }

            test("deduplicates entries across multiple vulnerabilities") {
                val entry1 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"))
                val entry2 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"))
                val input = mapOf(ReporterType.TRIVY to listOf(entry1, entry2))

                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).outputs
                val trivy = result.first() as SuppressionOutput.TrivySuppression

                trivy.entries shouldHaveSize 1
            }
        }

        context("buildSuppressionOutputs for Snyk") {

            test("maps snyk suppressions to SnykSuppression output") {
                val entry = suppressedVuln(id = VulnId.Snyk("SNYK-JAVA-001"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), input).outputs

                result shouldHaveSize 1
                result.first().shouldBeInstanceOf<SuppressionOutput.SnykSuppression>()
            }

            test("produces correct snyk entries from Snyk vulnId") {
                val entry =
                    suppressedVuln(
                        id = VulnId.Snyk("SNYK-JAVA-001"),
                        reporter = ReporterType.SNYK,
                        analysis = "not exploitable",
                    )
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), input).outputs
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries shouldHaveSize 1
                snyk.entries.first().id shouldBe VulnId.Snyk("SNYK-JAVA-001")
                snyk.entries.first().reason shouldBe "not exploitable"
            }

            test("filters out non-snyk vuln ids for Snyk output") {
                val cveEntry = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(cveEntry))

                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), input).outputs
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries.shouldBeEmpty()
            }

            test("produces empty SnykSuppression when no suppressions match") {
                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), emptyMap()).outputs

                result shouldHaveSize 1
                val snyk = result.first() as SuppressionOutput.SnykSuppression
                snyk.entries.shouldBeEmpty()
            }

            test("propagates expiresAt to snyk entries") {
                val expiresAt = LocalDate.of(2026, 12, 31)
                val entry =
                    suppressedVuln(
                        id = VulnId.Snyk("SNYK-JAVA-001"),
                        reporter = ReporterType.SNYK,
                        expiresAt = expiresAt,
                    )
                val input = mapOf(ReporterType.SNYK to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), input).outputs
                val snyk = result.first() as SuppressionOutput.SnykSuppression

                snyk.entries.first().expiresAt shouldBe expiresAt
            }
        }

        context("buildSuppressionOutputs for multiple reporters") {

            test("produces both trivy and snyk output") {
                val result = buildSuppressionOutputs(setOf(ReporterType.TRIVY, ReporterType.SNYK), emptyMap()).outputs

                result shouldHaveSize 2
            }
        }

        context("buildSuppressionOutputs format selection") {

            test("auto keeps the native format for a native reporter") {
                val input = mapOf(ReporterType.TRIVY to listOf(suppressedVuln()))

                val result =
                    buildSuppressionOutputs(setOf(ReporterType.TRIVY), input, SuppressionFormatRequest.Auto).outputs

                result.first().shouldBeInstanceOf<SuppressionOutput.TrivySuppression>()
            }

            test("auto falls back to generic for a reporter without a native format") {
                val input = mapOf(ReporterType.GRYPE to listOf(suppressedVuln(reporter = ReporterType.GRYPE)))

                val result =
                    buildSuppressionOutputs(setOf(ReporterType.GRYPE), input, SuppressionFormatRequest.Auto).outputs

                val generic = result.first().shouldBeInstanceOf<SuppressionOutput.GenericSuppression>()
                generic.fileName shouldBe "grype.generic.json"
            }

            test("generic filenames use canonical reporter names") {
                val result =
                    buildSuppressionOutputs(
                        setOf(ReporterType.GITHUB_DEPENDABOT),
                        emptyMap(),
                        SuppressionFormatRequest.Generic,
                    )

                val generic = result.outputs.first().shouldBeInstanceOf<SuppressionOutput.GenericSuppression>()
                generic.fileName shouldBe "github-dependabot.generic.json"
            }

            test("generic overrides the native format of a native reporter") {
                val entry = suppressedVuln(id = VulnId.Cve("CVE-2024-1234"), analysis = "false positive")
                val input = mapOf(ReporterType.TRIVY to listOf(entry))

                val result =
                    buildSuppressionOutputs(setOf(ReporterType.TRIVY), input, SuppressionFormatRequest.Generic).outputs

                val generic = result.first().shouldBeInstanceOf<SuppressionOutput.GenericSuppression>()
                generic.fileName shouldBe "trivy.generic.json"
                generic.entries shouldHaveSize 1
                generic.entries.first().id shouldBe VulnId.Cve("CVE-2024-1234")
                generic.entries.first().reason shouldBe "false positive"
            }

            test("generic widens the accepted vuln id types beyond the native format") {
                val cve = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.CARGO_AUDIT)
                val input = mapOf(ReporterType.CARGO_AUDIT to listOf(cve))

                val native =
                    buildSuppressionOutputs(setOf(ReporterType.CARGO_AUDIT), input, SuppressionFormatRequest.Auto)
                        .outputs
                native
                    .first()
                    .shouldBeInstanceOf<SuppressionOutput.CargoAuditSuppression>()
                    .entries
                    .shouldBeEmpty()

                val generic =
                    buildSuppressionOutputs(setOf(ReporterType.CARGO_AUDIT), input, SuppressionFormatRequest.Generic)
                        .outputs
                generic
                    .first()
                    .shouldBeInstanceOf<SuppressionOutput.GenericSuppression>()
                    .entries shouldHaveSize 1
            }

            test("excludes the OTHER reporter regardless of requested format") {
                val input = mapOf(ReporterType.OTHER to listOf(suppressedVuln(reporter = ReporterType.OTHER)))

                val result =
                    buildSuppressionOutputs(setOf(ReporterType.OTHER), input, SuppressionFormatRequest.Generic).outputs

                result.shouldBeEmpty()
            }

            test("generic produces one generic file per reporter") {
                val result =
                    buildSuppressionOutputs(
                        setOf(ReporterType.TRIVY, ReporterType.SNYK),
                        emptyMap(),
                        SuppressionFormatRequest.Generic,
                    ).outputs

                result shouldHaveSize 2
                result.filterIsInstance<SuppressionOutput.GenericSuppression>().map { it.fileName }.toSet() shouldBe
                    setOf("trivy.generic.json", "snyk.generic.json")
            }
        }

        context("buildSuppressionOutputs exclusions") {

            test("records a non-snyk id dropped from the snyk output") {
                val cveEntry = suppressedVuln(id = VulnId.Cve("CVE-2026-1234"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(cveEntry))

                val result = buildSuppressionOutputs(setOf(ReporterType.SNYK), input)

                result.exclusions shouldBe
                    listOf(
                        SuppressionExclusion.UnsupportedIdType(
                            id = VulnId.Cve("CVE-2026-1234"),
                            fileName = ".snyk",
                            format = SuppressionFormat.NativeFormat.Snyk,
                        ),
                    )
            }

            test("records entries of the OTHER reporter") {
                val entry = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.OTHER)
                val input = mapOf(ReporterType.OTHER to listOf(entry))

                val result = buildSuppressionOutputs(setOf(ReporterType.OTHER), input)

                result.outputs.shouldBeEmpty()
                result.exclusions shouldBe
                    listOf(
                        SuppressionExclusion.UnsupportedReporter(VulnId.Cve("CVE-2024-0001"), ReporterType.OTHER),
                    )
            }

            test("supported entries produce no exclusions") {
                val input = mapOf(ReporterType.TRIVY to listOf(suppressedVuln()))

                buildSuppressionOutputs(setOf(ReporterType.TRIVY), input).exclusions.shouldBeEmpty()
            }

            test("deduplicates identical exclusions") {
                val entry1 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.SNYK)
                val entry2 = suppressedVuln(id = VulnId.Cve("CVE-2024-0001"), reporter = ReporterType.SNYK)
                val input = mapOf(ReporterType.SNYK to listOf(entry1, entry2))

                buildSuppressionOutputs(setOf(ReporterType.SNYK), input).exclusions shouldHaveSize 1
            }
        }
    })
