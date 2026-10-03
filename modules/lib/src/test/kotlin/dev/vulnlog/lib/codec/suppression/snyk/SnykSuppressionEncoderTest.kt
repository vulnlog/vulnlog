// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import java.time.LocalDate

class SnykSuppressionEncoderTest :
    FunSpec({

        test("writes Snyk policy version and date-time expires value") {
            val input =
                SuppressionList(
                    format = SuppressionFormat.Snyk,
                    entries =
                        setOf(
                            SuppressionEntry(
                                id = VulnId.Snyk("SNYK-JS-EXAMPLE-1234567"),
                                expiresAt = LocalDate.of(2026, 8, 1),
                                reason = "temp suppression",
                            ),
                        ),
                )

            val result = SnykSuppressionEncoder.encode(input)

            result shouldContain "version: v1.25.0"
            result shouldContain "expires: \"2026-08-01T00:00:00.000Z\""
        }
    })
