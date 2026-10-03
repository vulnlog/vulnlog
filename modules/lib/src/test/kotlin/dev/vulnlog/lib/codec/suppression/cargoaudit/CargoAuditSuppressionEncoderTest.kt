// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.cargoaudit

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CargoAuditSuppressionEncoderTest :
    FunSpec({

        test("writes every entry in order") {
            val input =
                SuppressionList(
                    format = SuppressionFormat.CargoAudit,
                    entries =
                        setOf(
                            SuppressionEntry(id = VulnId.RustSec("RUSTSEC-2024-0001")),
                            SuppressionEntry(id = VulnId.RustSec("RUSTSEC-2021-0073")),
                        ),
                )

            val result = CargoAuditSuppressionEncoder.encode(input)

            result shouldBe
                """
                [advisories]
                ignore = [
                    "RUSTSEC-2024-0001",
                    "RUSTSEC-2021-0073",
                ]

                """.trimIndent()
        }

        test("writes empty ignore list") {
            val input = SuppressionList(SuppressionFormat.CargoAudit, emptySet())

            val result = CargoAuditSuppressionEncoder.encode(input)

            result shouldBe "[advisories]\nignore = []\n"
        }
    })
