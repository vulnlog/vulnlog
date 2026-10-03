// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.codec.suppression.snyk.dto.SnykIgnoreEntryDto
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionOutput
import dev.vulnlog.lib.model.suppression.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class SnykMapperTest :
    FunSpec({

        test("maps each entry to a wildcard path under its own id, with or without an expiry") {
            val input =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-001"),
                                reason = "not exploitable",
                                expiresAt = LocalDate.of(2026, 12, 31),
                            ),
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-002"),
                                reason = "permanent",
                            ),
                        ),
                )

            val dto = SnykMapper.toDto(input)

            val expiring = SnykIgnoreEntryDto("not exploitable", LocalDateTime.of(2026, 12, 31, 0, 0))
            val permanent = SnykIgnoreEntryDto("permanent", null)
            dto.ignore shouldBe
                mapOf(
                    "SNYK-JAVA-001" to listOf(mapOf("*" to expiring)),
                    "SNYK-JAVA-002" to listOf(mapOf("*" to permanent)),
                )
        }

        test("maps no entries to an empty ignore map") {
            val input = SuppressionOutput.SnykSuppression(entries = emptySet())

            val dto = SnykMapper.toDto(input)

            dto.ignore.shouldBeEmpty()
        }
    })
