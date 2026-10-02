// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.finding

import dev.vulnlog.lib.finding.FindingSeverity.ERROR
import dev.vulnlog.lib.finding.FindingSeverity.INFO
import dev.vulnlog.lib.finding.FindingSeverity.WARNING
import dev.vulnlog.lib.fixtures.finding
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidationFindingTest :
    FunSpec({

        test("highestSeverity is the most severe one in any order, and INFO without findings") {
            val severities = listOf(emptyList(), listOf(INFO, ERROR, WARNING), listOf(INFO, WARNING))

            val highest = severities.map { list -> list.map { finding(it) }.highestSeverity }

            highest shouldBe listOf(INFO, ERROR, WARNING)
        }
    })
