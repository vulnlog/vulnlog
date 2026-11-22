// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.report

import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.reporting.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant

class ReportFieldsTest :
    FunSpec({

        test("writes the generation time to the second, in UTC") {
            val generatedAt = Instant.parse("2026-11-22T14:32:05.476110281Z")

            val formatted = formatGeneratedAt(generatedAt)

            formatted shouldBe "2026-11-22T14:32:05Z"
        }

        test("labels the impact as the readable reports name it") {
            val impacts =
                listOf(
                    Impact.Affected(Severity.CRITICAL),
                    Impact.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    Impact.Unknown,
                )

            val labels = impacts.map(::labelImpact)

            labels shouldContainExactly listOf("critical", "not affected", "not triaged")
        }

        test("labels the verdict, the severity and the justification with the tokens of the Vulnlog file") {
            val impacts =
                listOf(
                    Impact.Affected(Severity.CRITICAL),
                    Impact.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    Impact.Unknown,
                )

            val labels = impacts.map { listOf(labelVerdict(it), labelSeverity(it), labelJustification(it)) }

            labels shouldContainExactly
                listOf(
                    listOf("affected", "critical", null),
                    listOf("not affected", null, "vulnerable code not in execute path"),
                    listOf(null, null, null),
                )
        }
    })
