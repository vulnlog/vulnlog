// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.util.UUID

class OpenVexVocabularyTest :
    FunSpec({

        context("vocabulary") {

            test("every status maps to its OpenVEX token") {
                openVexStatus(VexStatus.UnderInvestigation()) shouldBe "under_investigation"
                openVexStatus(VexStatus.Fixed) shouldBe "fixed"
                openVexStatus(VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)) shouldBe "not_affected"
                openVexStatus(VexStatus.Affected(Remediation.UpdateTo(release("1.0.1")))) shouldBe
                    "affected"
            }

            test("every justification maps to its OpenVEX token") {
                VexJustification.entries.map(::openVexJustification) shouldContainExactly
                    listOf(
                        "component_not_present",
                        "inline_mitigations_already_exist",
                        "vulnerable_code_cannot_be_controlled_by_adversary",
                        "vulnerable_code_not_in_execute_path",
                        "vulnerable_code_not_present",
                    )
            }
        }

        context("openVexVulnerabilityUrl") {

            test("points at the authority that issued the id") {
                openVexVulnerabilityUrl(cve("CVE-2021-44228")) shouldBe
                    "https://nvd.nist.gov/vuln/detail/CVE-2021-44228"
                openVexVulnerabilityUrl(ghsa("GHSA-jfh8-c2jp-5v3q")) shouldBe
                    "https://github.com/advisories/GHSA-jfh8-c2jp-5v3q"
                openVexVulnerabilityUrl(VulnId.RustSec("RUSTSEC-2021-0001")) shouldBe
                    "https://rustsec.org/advisories/RUSTSEC-2021-0001"
                openVexVulnerabilityUrl(VulnId.Snyk("SNYK-JS-LODASH-567746")) shouldBe
                    "https://security.snyk.io/vuln/SNYK-JS-LODASH-567746"
            }
        }

        context("openVexDocumentId") {

            test("names the given UUID under the Vulnlog namespace") {
                val uuid = UUID.fromString("3e671687-395b-41f5-a30f-a58921a69b79")

                openVexDocumentId(uuid) shouldBe
                    OpenVexDocumentId("https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79")
            }
        }
    })
