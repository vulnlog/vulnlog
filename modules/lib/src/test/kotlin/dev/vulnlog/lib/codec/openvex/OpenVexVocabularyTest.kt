// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatusKind
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID

private val everyRemediation =
    listOf(
        Remediation.RiskAccepted(fixIn = null),
        Remediation.RiskAccepted(fixIn = release("1.0.1")),
        Remediation.FixPlanned(fixIn = null),
        Remediation.FixPlanned(fixIn = release("1.0.1")),
        Remediation.UpdateTo(release("1.0.1")),
        Remediation.NoneAvailable,
    )

class OpenVexVocabularyTest :
    FunSpec({

        context("status and justification") {

            test("every status has its OpenVEX token and is read back from it") {
                val tokens = VexStatusKind.entries.map(::openVexStatus)

                val kinds = tokens.map(::openVexStatusKind)

                tokens shouldContainExactly listOf("affected", "fixed", "not_affected", "under_investigation")
                kinds shouldContainExactly VexStatusKind.entries
            }

            test("every justification has its OpenVEX token and is read back from it") {
                val tokens = VexJustification.entries.map(::openVexJustification)

                val justifications = tokens.map(::openVexJustificationOf)

                tokens shouldContainExactly
                    listOf(
                        "component_not_present",
                        "inline_mitigations_already_exist",
                        "vulnerable_code_cannot_be_controlled_by_adversary",
                        "vulnerable_code_not_in_execute_path",
                        "vulnerable_code_not_present",
                    )
                justifications shouldContainExactly VexJustification.entries
            }
        }

        context("action statement") {

            test("words every remediation and reads it back") {
                val sentences = everyRemediation.map(::openVexActionStatement)

                val remediations = sentences.map(::openVexRemediation)

                sentences shouldContainExactly
                    listOf(
                        "The risk is accepted. No fix is planned.",
                        "The risk is accepted for this release. A fix ships with release 1.0.1.",
                        "A fix is planned but not yet available.",
                        "A fix is planned for release 1.0.1.",
                        "Update to release 1.0.1.",
                        "No remediation is available yet.",
                    )
                remediations shouldContainExactly everyRemediation
            }

            test("reads nothing from a text Vulnlog never writes") {
                val remediation = openVexRemediation("Upgrade to the latest version.")

                remediation.shouldBeNull()
            }
        }

        context("@context") {

            test("round trips every format version") {
                val contexts = OpenVexFormatVersion.entries.map(::openVexContext)

                val declared = contexts.map(::declaredOpenVexVersion)

                contexts.first() shouldBe "https://openvex.dev/ns/v0.2.0"
                declared shouldContainExactly OpenVexFormatVersion.entries.map(OpenVexFormatVersion::version)
            }

            test("tells an unknown version apart from another format") {
                val contexts =
                    listOf(
                        "https://openvex.dev/ns/v9.9.9",
                        "https://openvex.dev/ns",
                        "https://openvex.dev/ns/v",
                        "https://cyclonedx.org/schema",
                    )

                val declared = contexts.map(::declaredOpenVexVersion)

                declared shouldContainExactly listOf("9.9.9", "0.0.1", null, null)
            }
        }

        test("a vulnerability URL points at the authority that issued the id") {
            val ids =
                listOf(
                    cve("CVE-2021-44228"),
                    ghsa("GHSA-jfh8-c2jp-5v3q"),
                    VulnId.RustSec("RUSTSEC-2021-0001"),
                    VulnId.Snyk("SNYK-JS-LODASH-567746"),
                )

            val urls = ids.map(::openVexVulnerabilityUrl)

            urls shouldContainExactly
                listOf(
                    "https://nvd.nist.gov/vuln/detail/CVE-2021-44228",
                    "https://github.com/advisories/GHSA-jfh8-c2jp-5v3q",
                    "https://rustsec.org/advisories/RUSTSEC-2021-0001",
                    "https://security.snyk.io/vuln/SNYK-JS-LODASH-567746",
                )
        }

        test("a document id names the UUID under the Vulnlog namespace") {
            val uuid = UUID.fromString("3e671687-395b-41f5-a30f-a58921a69b79")

            val id = openVexDocumentId(uuid)

            id shouldBe OpenVexDocumentId("https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79")
        }
    })
