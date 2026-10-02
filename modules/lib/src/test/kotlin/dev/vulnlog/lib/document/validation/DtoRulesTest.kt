// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.finding.Rule
import dev.vulnlog.lib.finding.ValidationFinding
import dev.vulnlog.lib.fixtures.v1Dto
import dev.vulnlog.lib.fixtures.vulnerabilityDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun applyV1DtoRules(dto: VulnlogFileV1Dto): List<ValidationFinding> = v1DtoRules.flatMap { rule -> rule(dto) }

class DtoRulesTest :
    FunSpec({

        context("deprecated verdict") {

            test("a 'risk acceptable' verdict is a warning that names the replacement") {
                val dto = v1Dto(vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1", verdict = "risk acceptable")))

                val findings = applyV1DtoRules(dto)

                with(findings.single()) {
                    severity shouldBe FindingSeverity.WARNING
                    rule shouldBe Rule.DEPRECATED_VERDICT
                    path shouldBe "vulnerabilities[CVE-2021-1].verdict"
                    message shouldContain "disposition 'wont fix'"
                }
            }

            test("each deprecated entry is reported on its own, and no other entry") {
                val dto =
                    v1Dto(
                        vulnerabilities =
                            listOf(
                                vulnerabilityDto("CVE-2021-1", verdict = "risk acceptable"),
                                vulnerabilityDto("CVE-2021-2", verdict = "affected"),
                                vulnerabilityDto("CVE-2021-3"),
                                vulnerabilityDto("CVE-2021-4", verdict = "risk acceptable"),
                            ),
                    )

                val findings = applyV1DtoRules(dto)

                findings.map { it.path } shouldBe
                    listOf("vulnerabilities[CVE-2021-1].verdict", "vulnerabilities[CVE-2021-4].verdict")
            }
        }
    })
