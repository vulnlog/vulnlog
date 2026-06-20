// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SuppressionFormatRequestTest :
    FunSpec({

        context("fromToken") {

            test("parses auto") {
                SuppressionFormatRequest.fromToken("auto") shouldBe SuppressionFormatRequest.Auto
            }

            test("parses generic") {
                SuppressionFormatRequest.fromToken("generic") shouldBe SuppressionFormatRequest.Generic
            }

            test("is case insensitive") {
                SuppressionFormatRequest.fromToken("AUTO") shouldBe SuppressionFormatRequest.Auto
                SuppressionFormatRequest.fromToken("Generic") shouldBe SuppressionFormatRequest.Generic
            }

            test("rejects an unknown token with a helpful message") {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        SuppressionFormatRequest.fromToken("xml")
                    }

                val message = exception.message.shouldNotBeNull()
                message shouldContain "Unknown suppression format 'xml'"
                message shouldContain "auto"
                message shouldContain "generic"
            }
        }

        test("byToken exposes the supported tokens") {
            SuppressionFormatRequest.byToken.keys shouldBe setOf("auto", "generic")
        }
    })
