// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SuppressionFormatRequestTest :
    FunSpec({

        test("byToken names every format, and fromToken parses each token in any case") {
            val tokens = listOf("auto", "generic", "AUTO", "Generic")

            val requests = tokens.map(SuppressionFormatRequest::fromToken)

            SuppressionFormatRequest.byToken.keys shouldBe setOf("auto", "generic")
            requests shouldBe
                listOf(
                    SuppressionFormatRequest.Auto,
                    SuppressionFormatRequest.Generic,
                    SuppressionFormatRequest.Auto,
                    SuppressionFormatRequest.Generic,
                )
        }

        test("fromToken rejects an unknown token and lists the valid ones") {
            val failure = shouldThrow<IllegalArgumentException> { SuppressionFormatRequest.fromToken("xml") }

            failure.message shouldBe "Unknown suppression format 'xml'. Valid values: auto, generic."
        }
    })
