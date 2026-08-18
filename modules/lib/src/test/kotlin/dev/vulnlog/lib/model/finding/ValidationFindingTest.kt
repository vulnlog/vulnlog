// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.finding

import dev.vulnlog.lib.fixtures.finding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidationFindingTest :
    FunSpec({

        context("highestSeverity") {

            test("no findings count as informational") {
                val findings = emptyList<ValidationFinding>()

                val highest = findings.highestSeverity

                highest shouldBe FindingSeverity.INFO
            }

            test("a single finding reports its own severity") {
                val findings = listOf(finding(FindingSeverity.WARNING))

                val highest = findings.highestSeverity

                highest shouldBe FindingSeverity.WARNING
            }

            test("an error outranks warnings and infos regardless of order") {
                val findings =
                    listOf(
                        finding(FindingSeverity.INFO),
                        finding(FindingSeverity.ERROR),
                        finding(FindingSeverity.WARNING),
                    )

                val highest = findings.highestSeverity

                highest shouldBe FindingSeverity.ERROR
            }

            test("a warning outranks infos") {
                val findings = listOf(finding(FindingSeverity.INFO), finding(FindingSeverity.WARNING))

                val highest = findings.highestSeverity

                highest shouldBe FindingSeverity.WARNING
            }
        }
    })
