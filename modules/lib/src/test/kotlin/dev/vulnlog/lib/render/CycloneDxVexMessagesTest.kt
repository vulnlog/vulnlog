// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.CycloneDxVexOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexStatementTime
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexCollection
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexEmptyReason
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentityField
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexSkippedEntry
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val lib = Purl.Npm("pkg:npm/example-lib@2.3.0")

private fun vulnerability(
    id: String,
    status: VexStatus,
): CycloneDxVexVulnerability =
    CycloneDxVexVulnerability(cve(id), emptyList(), null, listOf(lib), status, VexStatementTime.Issued)

private val collection =
    CycloneDxVexCollection(
        scope = CycloneDxVexScope(release("1.2.0"), setOf(tag("container"))),
        vulnerabilities =
            listOf(
                vulnerability("CVE-2026-1111", VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)),
                vulnerability("CVE-2026-2222", VexStatus.Affected(Remediation.NoneAvailable)),
                vulnerability("CVE-2026-3333", VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)),
            ),
        productPurls = listOf(Purl.Docker("pkg:docker/acme/web-app@1.2.0")),
        skippedEntries =
            listOf(
                CycloneDxVexSkippedEntry.NotOnRelease(cve("CVE-2026-5555")),
                CycloneDxVexSkippedEntry.NoRelease(cve("CVE-2026-4444")),
            ),
    )

private val empty =
    collection.copy(
        vulnerabilities = emptyList(),
        productPurls = emptyList(),
        skippedEntries = emptyList(),
    )

class CycloneDxVexMessagesTest :
    FunSpec({

        context("renderCycloneDxVexMessages") {

            test("reports the scope, the product, the skipped entries and the counts, in print order") {
                val messages =
                    renderCycloneDxVexMessages(
                        CycloneDxVexOutcome.Unchanged(collection, VexDocumentVersion.FIRST, "{}"),
                    )

                messages shouldContainExactly
                    listOf(
                        Message.Verbose("release scope: 1.2.0"),
                        Message.Verbose("tag scope matched tags: container"),
                        Message.Verbose("product: release '1.2.0' with 1 purl: pkg:docker/acme/web-app@1.2.0"),
                        Message.Debug("skipped CVE-2026-4444: it references no release"),
                        Message.Debug("skipped CVE-2026-5555: it does not apply to the release in scope"),
                        Message.Verbose("collected 3 vulnerabilities: 1 affected, 2 not_affected"),
                    )
            }

            test("reports no count and no product when nothing applies") {
                val outcome =
                    CycloneDxVexOutcome.NoVulnerabilityApplies(
                        empty,
                        CycloneDxVexEmptyReason.RELEASE_DECLARES_NO_PURLS,
                    )

                renderCycloneDxVexMessages(outcome) shouldContainExactly
                    listOf(
                        Message.Verbose("release scope: 1.2.0"),
                        Message.Verbose("tag scope matched tags: container"),
                    )
            }

            test("reports nothing for a run rejected before it collected") {
                val rejected =
                    listOf(
                        FilterRejected(listOf(FilterProblem.UnknownRelease(release("9.9.9"), emptyList()))),
                        CycloneDxVexOutcome.BaselineRejected(CycloneDxVexBaselineProblem.NotCycloneDx),
                    )

                rejected.flatMap(::renderCycloneDxVexMessages).shouldBeEmpty()
            }
        }

        test("renderCycloneDxVexWritten names the target, the format, the version and the count") {
            val message =
                renderCycloneDxVexWritten(
                    "vex.cdx.json",
                    CycloneDxVexOutcome.Unchanged(collection, VexDocumentVersion(4), "{}"),
                )

            message shouldBe Message.Verbose("wrote vex.cdx.json: cyclonedx format, version 4, 3 vulnerabilities")
        }

        test("renderCycloneDxVexFailure names the baseline, what is wrong with it and the option to omit") {
            val problems =
                listOf(
                    CycloneDxVexBaselineProblem.NotCycloneDx,
                    CycloneDxVexBaselineProblem.OtherFormatVersion("1.6", CycloneDxVexFormatVersion.VERSION_1_7),
                    CycloneDxVexBaselineProblem.InvalidIdentity(CycloneDxVexIdentityField.SERIAL_NUMBER, null),
                    CycloneDxVexBaselineProblem.InvalidIdentity(CycloneDxVexIdentityField.TIMESTAMP, "yesterday"),
                    CycloneDxVexBaselineProblem.InvalidIdentity(CycloneDxVexIdentityField.VERSION, "0"),
                )

            val failures =
                problems.flatMap { problem ->
                    renderCycloneDxVexFailure(
                        CycloneDxVexOutcome.BaselineRejected(problem),
                        "vex.cdx.json",
                        "--baseline",
                        "--tag",
                    )
                }

            failures.map { it.message } shouldContainExactly
                listOf(
                    "baseline 'vex.cdx.json' is not a CycloneDX document",
                    "baseline 'vex.cdx.json' is a CycloneDX 1.6 document, but this run writes CycloneDX 1.7",
                    "baseline 'vex.cdx.json' has no 'serialNumber', expected a urn:uuid",
                    "baseline 'vex.cdx.json' has an invalid 'metadata.timestamp' 'yesterday', expected an RFC 3339 timestamp",
                    "baseline 'vex.cdx.json' has an invalid 'version' '0', expected a whole number from 1 to ${Int.MAX_VALUE - 1}",
                )
            failures.map { it.hint }.toSet() shouldBe setOf("omit --baseline to issue a new document")
        }

        test("renderCycloneDxVexFailure says that no vulnerability applies and names what to change for every reason") {
            val failures =
                CycloneDxVexEmptyReason.entries.map { reason ->
                    renderCycloneDxVexFailure(
                        CycloneDxVexOutcome.NoVulnerabilityApplies(empty, reason),
                        "",
                        "--baseline",
                        "--tag",
                    ).single()
                }

            failures.map { it.message }.toSet() shouldBe setOf("no vulnerability applies")
            failures.map { it.hint } shouldContainExactly
                listOf(
                    "declare 'purls' on release '1.2.0'",
                    "no purl of release '1.2.0' carries one of the requested tags, pass another --tag or omit it",
                    "tag the vulnerability entries with the tags of the release purls they apply to",
                    "no vulnerability entry and release purl in scope share one of the requested tags",
                    "no vulnerability entry applies to release '1.2.0'",
                )
        }
    })
