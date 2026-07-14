// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class SnykSuppressionWriterTest :
    FunSpec({

        test("writes Snyk policy version and date-time expires value") {
            val input =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JS-EXAMPLE-1234567"),
                                expiresAt = LocalDate.of(2026, 8, 1),
                                reason = "temp suppression",
                            ),
                        ),
                )

            val result = SnykSuppressionWriter.write(input)

            result shouldContain "version: v1.25.0"
            result shouldContain "expires: 2026-08-01T00:00:00.000Z"
        }
    })
