// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImpactReportMessagesTest :
    FunSpec({

        context("renderReportingCounts") {

            test("states the collected and merged counts") {
                renderReportingCounts(collected = 12, merged = 9) shouldBe
                    "collected 12 report entries, merged to 9"
            }
        }
    })
