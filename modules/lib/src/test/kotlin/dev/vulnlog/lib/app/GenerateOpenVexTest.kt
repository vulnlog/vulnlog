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
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexStatementTime
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant
import java.time.LocalDate

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")
private val DOCUMENT_ID = OpenVexDocumentId("https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79")

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
                vulnerability(id = cve("CVE-2026-1111"), releases = releases.map(::release), tags = listOf(tag("app"))),
            ),
    )

/** Through YAML and the load step, as a driver hands it over: only the load step builds a [ValidVulnlogProject]. */
private fun loaded(file: VulnlogFile): ValidVulnlogProject = validated(YamlWriter.write(file))

private fun request(
    baseline: String? = null,
    now: Instant = ISSUED_AT,
    release: String? = null,
): OpenVexRequest = OpenVexRequest(release, emptySet(), baseline, DOCUMENT_ID, now, OpenVexTooling("CLI", "0.18.0"))

private fun revised(
    file: VulnlogFile,
    baseline: String? = null,
    now: Instant = ISSUED_AT,
    release: String? = null,
): OpenVexOutcome.Revised = generateOpenVex(loaded(file), request(baseline, now, release)).shouldBeInstanceOf()

class GenerateOpenVexTest :
    FunSpec({

        test("a run without a baseline issues the first version under the drawn id") {
            val file = fileWith("1.0.0")

            val revised = revised(file)

            revised.document.identity shouldBe OpenVexIdentity(DOCUMENT_ID, ISSUED_AT, OpenVexDocumentVersion.FIRST)
            revised.version shouldBe OpenVexDocumentVersion.FIRST
        }

        test("a rerun over an unchanged file keeps the baseline bytes and version") {
            val first = revised(fileWith("1.0.0"))

            val outcome = generateOpenVex(loaded(fileWith("1.0.0")), request(first.content, UPDATED_AT))

            outcome shouldBe OpenVexOutcome.Unchanged(first.collection, OpenVexDocumentVersion.FIRST, first.content)
        }

        test("a changed file continues the identity and counts the version up") {
            val first = revised(fileWith("1.0.0"))

            val second = revised(fileWith("1.0.0", "1.1.0"), first.content, UPDATED_AT)

            second.document.identity shouldBe OpenVexIdentity(DOCUMENT_ID, UPDATED_AT, OpenVexDocumentVersion(2))
        }

        test("an undated statement keeps its time when another entry changes the document") {
            val file = fileWith("1.0.0", publishedAt = null)
            val first = revised(file, release = "1.0.0")
            val added =
                vulnerability(
                    id = cve("CVE-2026-2222"),
                    releases = listOf(release("1.0.0")),
                    analyzedAt = LocalDate.of(2026, 4, 30),
                    verdict = Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT),
                    tags = listOf(tag("app")),
                )

            val second =
                revised(file.copy(vulnerabilities = file.vulnerabilities + added), first.content, UPDATED_AT, "1.0.0")

            second.document.statements.map { it.vulnerability.id to it.timestamp } shouldContainExactly
                listOf(
                    cve("CVE-2026-1111") to OpenVexStatementTime.Carried(ISSUED_AT),
                    cve("CVE-2026-2222") to OpenVexStatementTime.Stated(LocalDate.of(2026, 4, 30)),
                )
            second.content shouldContain "\"timestamp\": \"2026-04-25T00:00:00Z\""
        }

        test("a scope the file does not define is rejected") {
            val file = fileWith("1.0.0")

            val outcome = generateOpenVex(loaded(file), request(release = "9.9.9"))

            outcome shouldBe
                FilterRejected(listOf(FilterProblem.UnknownRelease(release("9.9.9"), listOf(release("1.0.0")))))
        }

        test("a baseline that cannot be continued is rejected") {
            val baseline = """{"bomFormat": "CycloneDX"}"""

            val outcome = generateOpenVex(loaded(fileWith("1.0.0")), request(baseline = baseline))

            outcome shouldBe OpenVexOutcome.BaselineRejected(OpenVexBaselineProblem.NotOpenVex)
        }

        test("a file without an anchoring release yields no document") {
            val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

            val outcome = generateOpenVex(loaded(bare), request())

            outcome.shouldBeInstanceOf<OpenVexOutcome.NoStatementApplies>().reason shouldBe
                OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS
        }
    })
