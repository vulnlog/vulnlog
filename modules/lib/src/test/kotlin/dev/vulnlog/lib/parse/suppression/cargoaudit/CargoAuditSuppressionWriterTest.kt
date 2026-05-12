// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.cargoaudit

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CargoAuditSuppressionWriterTest :
    FunSpec({

        test("writes single RUSTSEC entry") {
            val input =
                SuppressionOutput.CargoAuditSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.CargoAuditSuppressionEntry(
                                id = VulnId.RustSec("RUSTSEC-2024-0001"),
                            ),
                        ),
                )

            val result = CargoAuditSuppressionWriter.write(input)

            result shouldBe
                """
                [advisories]
                ignore = [
                    "RUSTSEC-2024-0001",
                ]

                """.trimIndent()
        }

        test("writes multiple entries") {
            val input =
                SuppressionOutput.CargoAuditSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.CargoAuditSuppressionEntry(id = VulnId.RustSec("RUSTSEC-2024-0001")),
                            SuppressionVuln.CargoAuditSuppressionEntry(id = VulnId.RustSec("RUSTSEC-2021-0073")),
                        ),
                )

            val result = CargoAuditSuppressionWriter.write(input)

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
            val input = SuppressionOutput.CargoAuditSuppression(entries = emptySet())

            val result = CargoAuditSuppressionWriter.write(input)

            result shouldBe "[advisories]\nignore = []\n"
        }
    })
