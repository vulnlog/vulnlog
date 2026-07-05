// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.trivy

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class TrivySuppressionWriterTest :
    FunSpec({

        test("writes Trivy expiration key as expired_at") {
            val input =
                SuppressionOutput.TrivySuppression(
                    entries =
                        setOf(
                            SuppressionVuln.TrivySuppressionEntry(
                                id = VulnId.Cve("CVE-2026-1111"),
                                expiresAt = LocalDate.of(2026, 8, 1),
                                reason = "temp suppression while fix pending",
                            ),
                        ),
                )

            val result = TrivySuppressionWriter.write(input)

            result shouldContain "expired_at: 2026-08-01"
            result shouldNotContain "expires_at"
        }
    })
