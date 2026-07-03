// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.result.ParseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParseRendererTest :
    FunSpec({

        test("renders the failure with its location") {
            val rendered = renderParseFailure("a.vl.yaml", ParseResult.Error("boom", FailureLocation(3, 5)))

            rendered shouldBe "Parsing of a.vl.yaml failed:\n[ERROR] Line 3:5 - boom"
        }

        test("renders the failure without a position when the location is unknown") {
            val rendered = renderParseFailure("a.vl.yaml", ParseResult.Error("boom"))

            rendered shouldBe "Parsing of a.vl.yaml failed:\n[ERROR] boom"
        }
    })
