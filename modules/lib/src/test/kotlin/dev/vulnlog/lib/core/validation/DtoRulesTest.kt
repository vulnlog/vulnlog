// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.fixtures.v1Dto
import dev.vulnlog.lib.fixtures.vulnerabilityDto
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.model.finding.ValidationFinding
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/** The DTO rules see the raw tokens, so they can still name spellings the domain normalizes away. */
private fun applyV1DtoRules(dto: VulnlogFileV1Dto): List<ValidationFinding> = v1DtoRules.flatMap { rule -> rule(dto) }

class DtoRulesTest :
    FunSpec({

        context("deprecated verdict") {

            test("a 'risk acceptable' verdict is reported as a warning") {
                val dto =
                    v1Dto(
                        vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1", verdict = "risk acceptable")),
                    )

                val findings = applyV1DtoRules(dto)

                with(findings.single()) {
                    severity shouldBe FindingSeverity.WARNING
                    rule shouldBe Rule.DEPRECATED_VERDICT
                    path shouldBe "vulnerabilities[CVE-2021-1].verdict"
                    message shouldContain "disposition 'wont fix'"
                }
            }

            test("a supported verdict produces no finding") {
                val dto = v1Dto(vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1", verdict = "affected")))

                val findings = applyV1DtoRules(dto)

                findings.shouldBeEmpty()
            }

            test("a missing verdict produces no finding") {
                val dto = v1Dto(vulnerabilities = listOf(vulnerabilityDto("CVE-2021-1")))

                val findings = applyV1DtoRules(dto)

                findings.shouldBeEmpty()
            }

            test("every deprecated entry is reported on its own") {
                val dto =
                    v1Dto(
                        vulnerabilities =
                            listOf(
                                vulnerabilityDto("CVE-2021-1", verdict = "risk acceptable"),
                                vulnerabilityDto("CVE-2021-2", verdict = "affected"),
                                vulnerabilityDto("CVE-2021-3", verdict = "risk acceptable"),
                            ),
                    )

                val findings = applyV1DtoRules(dto)

                findings shouldHaveSize 2
                findings.map { it.path } shouldBe
                    listOf("vulnerabilities[CVE-2021-1].verdict", "vulnerabilities[CVE-2021-3].verdict")
            }
        }
    })
