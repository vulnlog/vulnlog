// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.document.validated
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.document.yaml.YamlWriter
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")
private val SERIAL_NUMBER = CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79")
private val LIB = Purl.Npm("pkg:npm/example-lib@2.3.0")

private fun fileWith(
    vararg releases: String,
    publishedAt: LocalDate? = LocalDate.of(2026, 4, 1),
): VulnlogFile =
    vulnlogFile(
        tags = listOf(tagEntry("app")),
        releases =
            releases.map { id ->
                val purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@$id", tags = listOf("app")))
                releaseEntry(id, purls = purls, publishedAt = publishedAt)
            },
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve("CVE-2026-1111"),
                    releases = releases.map(::release),
                    packages = listOf(LIB),
                    tags = listOf(tag("app")),
                ),
            ),
    )

/** Through YAML and the load step, as a driver hands it over: only the load step builds a [ValidVulnlogProject]. */
private fun loaded(file: VulnlogFile): ValidVulnlogProject = validated(YamlWriter.write(file))

private fun request(
    baseline: String? = null,
    now: Instant = ISSUED_AT,
    release: String = "1.0.0",
): CycloneDxVexRequest =
    CycloneDxVexRequest(release, emptySet(), baseline, SERIAL_NUMBER, now, VexTooling("CLI", "0.18.0"))

private fun revised(
    file: VulnlogFile,
    baseline: String? = null,
    now: Instant = ISSUED_AT,
    release: String = "1.0.0",
): CycloneDxVexOutcome.Revised =
    generateCycloneDxVex(loaded(file), request(baseline, now, release)).shouldBeInstanceOf()

class GenerateCycloneDxVexTest :
    FunSpec({

        test("a run without a baseline issues the first version under the drawn serial number") {
            val revised = revised(fileWith("1.0.0"))

            revised.document.identity shouldBe CycloneDxVexIdentity(SERIAL_NUMBER, ISSUED_AT, VexDocumentVersion.FIRST)
            revised.version shouldBe VexDocumentVersion.FIRST
        }

        test("a rerun over an unchanged file keeps the baseline bytes and version") {
            val first = revised(fileWith("1.0.0"))

            val outcome = generateCycloneDxVex(loaded(fileWith("1.0.0")), request(first.content, UPDATED_AT))

            outcome shouldBe CycloneDxVexOutcome.Unchanged(first.collection, VexDocumentVersion.FIRST, first.content)
        }

        test("a changed file continues the identity and counts the version up") {
            val first = revised(fileWith("1.0.0"))
            val changed =
                fileWith("1.0.0").let { file ->
                    file.copy(
                        vulnerabilities =
                            file.vulnerabilities.map {
                                it.copy(verdict = Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT))
                            },
                    )
                }

            val second = revised(changed, first.content, UPDATED_AT)

            second.document.identity shouldBe CycloneDxVexIdentity(SERIAL_NUMBER, UPDATED_AT, VexDocumentVersion(2))
        }

        test("an undated analysis keeps its time when another entry changes the document") {
            val file = fileWith("1.0.0", publishedAt = null)
            val first = revised(file)
            val added =
                vulnerability(
                    id = cve("CVE-2026-2222"),
                    releases = listOf(release("1.0.0")),
                    packages = listOf(LIB),
                    analyzedAt = LocalDate.of(2026, 4, 30),
                    verdict = Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT),
                    tags = listOf(tag("app")),
                )

            val second = revised(file.copy(vulnerabilities = file.vulnerabilities + added), first.content, UPDATED_AT)

            second.document.vulnerabilities.map { it.id to it.firstIssued } shouldContainExactly
                listOf(
                    cve("CVE-2026-1111") to VexStatementTime.Carried(ISSUED_AT),
                    cve("CVE-2026-2222") to VexStatementTime.Stated(LocalDate.of(2026, 4, 30)),
                )
            second.content shouldContain "\"firstIssued\": \"2026-04-25T00:00:00Z\""
        }

        test("a release the file does not define is rejected") {
            val file = fileWith("1.0.0")

            val outcome = generateCycloneDxVex(loaded(file), request(release = "9.9.9"))

            outcome shouldBe
                FilterRejected(listOf(FilterProblem.UnknownRelease(release("9.9.9"), listOf(release("1.0.0")))))
        }

        test("a baseline that cannot be continued is rejected") {
            val baseline = """{"@context": "https://openvex.dev/ns/v0.2.0"}"""

            val outcome = generateCycloneDxVex(loaded(fileWith("1.0.0")), request(baseline = baseline))

            outcome shouldBe CycloneDxVexOutcome.BaselineRejected(CycloneDxVexBaselineProblem.NotCycloneDx)
        }

        test("a release without purls yields no document") {
            val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

            val outcome = generateCycloneDxVex(loaded(bare), request())

            outcome.shouldBeInstanceOf<CycloneDxVexOutcome.NoVulnerabilityApplies>().reason shouldBe
                CycloneDxVexEmptyReason.RELEASE_DECLARES_NO_PURLS
        }
    })
