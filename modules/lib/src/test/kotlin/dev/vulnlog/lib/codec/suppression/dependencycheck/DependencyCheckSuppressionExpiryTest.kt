// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.codec.suppression.SuppressionEncoder
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEntry
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneOffset

class DependencyCheckSuppressionExpiryTest :
    FunSpec({
        listOf(
            "ordinary day" to LocalDate.of(2026, 8, 1),
            "month end" to LocalDate.of(2026, 4, 30),
            "leap day" to LocalDate.of(2028, 2, 29),
            "year end" to LocalDate.of(2026, 12, 31),
        ).forEach { (name, expiry) ->
            test("inclusive expiry remains active throughout $name: $expiry") {
                val xml = SuppressionEncoder.encode(expiring(expiry))
                val until = Regex("until=\"([^\"]+)Z\"").find(xml)!!.groupValues[1]
                val cutoff = LocalDate.parse(until).atStartOfDay().toInstant(ZoneOffset.UTC)
                val lastDay = expiry.atStartOfDay().toInstant(ZoneOffset.UTC)

                until shouldBe expiry.plusDays(1).toString()
                lastDay.isBefore(cutoff) shouldBe true
                lastDay.plusSeconds(86_399).isBefore(cutoff) shouldBe true
                lastDay.plusSeconds(86_400).isBefore(cutoff) shouldBe false
            }
        }

        test("the maximum expiry is rejected rather than overflowing or becoming unbounded") {
            shouldThrow<IllegalArgumentException> { SuppressionEncoder.encode(expiring(LocalDate.MAX)) }
        }
    })

private fun expiring(expiry: LocalDate) =
    SuppressionList(
        SuppressionFormat.DependencyCheck,
        setOf(SuppressionEntry(VulnId.Cve("CVE-2026-1111"), expiry)),
    )
