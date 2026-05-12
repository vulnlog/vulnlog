// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SnykMapperTest :
    FunSpec({

        test("maps entry to nested ignore structure with wildcard path") {
            val input =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-001"),
                                reason = "not exploitable",
                                expiresAt = LocalDate.of(2026, 12, 31),
                            ),
                        ),
                )

            val dto = SnykMapper.toDto(input)

            dto.ignore shouldHaveSize 1
            val entryList = dto.ignore["SNYK-JAVA-001"]!!
            entryList.size shouldBe 1
            val wildcardEntry = entryList.first()["*"]!!
            wildcardEntry.reason shouldBe "not exploitable"
            wildcardEntry.expires shouldBe LocalDate.of(2026, 12, 31)
        }

        test("maps multiple entries to separate ignore keys") {
            val input =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(id = VulnId.Snyk("SNYK-JAVA-001"), reason = "reason1"),
                            SuppressionVuln.SnykSuppressionEntry(id = VulnId.Snyk("SNYK-JAVA-002"), reason = "reason2"),
                        ),
                )

            val dto = SnykMapper.toDto(input)

            dto.ignore shouldHaveSize 2
        }

        test("maps empty entries to empty ignore map") {
            val input = SuppressionOutput.SnykSuppression(entries = emptySet())

            val dto = SnykMapper.toDto(input)

            dto.ignore.shouldBeEmpty()
        }

        test("maps entry without expiresAt") {
            val input =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-001"),
                                reason = "permanent",
                            ),
                        ),
                )

            val dto = SnykMapper.toDto(input)
            val wildcardEntry = dto.ignore["SNYK-JAVA-001"]!!.first()["*"]!!

            wildcardEntry.reason shouldBe "permanent"
            wildcardEntry.expires shouldBe null
        }
    })
