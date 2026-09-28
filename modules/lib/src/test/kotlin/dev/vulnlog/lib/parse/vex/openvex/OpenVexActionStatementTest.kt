// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.vex.Remediation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private val everyRemediation =
    listOf(
        Remediation.RiskAccepted(fixIn = null),
        Remediation.RiskAccepted(fixIn = Release("1.0.1")),
        Remediation.FixPlanned,
        Remediation.UpdateTo(Release("1.0.1")),
        Remediation.NoneAvailable,
    )

class OpenVexActionStatementTest :
    FunSpec({

        context("actionStatementOf") {

            test("words every remediation") {
                everyRemediation.map(::actionStatementOf) shouldBe
                    listOf(
                        "The risk is accepted. No fix is planned.",
                        "The risk is accepted for this release. A fix ships with release 1.0.1.",
                        "A fix is planned but not yet available.",
                        "Update to release 1.0.1.",
                        "No remediation is available yet.",
                    )
            }
        }

        context("remediationOf") {

            test("reads back every remediation it words") {
                everyRemediation.map { remediationOf(actionStatementOf(it)) } shouldBe everyRemediation
            }

            test("reads a release whose id holds periods") {
                remediationOf("Update to release 1.2.0.") shouldBe Remediation.UpdateTo(Release("1.2.0"))
            }

            test("reads nothing from a text it never writes") {
                remediationOf("Upgrade to the latest version.").shouldBeNull()
            }
        }
    })
