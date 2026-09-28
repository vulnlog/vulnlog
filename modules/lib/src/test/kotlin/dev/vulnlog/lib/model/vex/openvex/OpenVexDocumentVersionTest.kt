// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class OpenVexDocumentVersionTest :
    FunSpec({

        test("the first version is 1") {
            OpenVexDocumentVersion.FIRST.value shouldBe 1
        }

        test("the next version is one more") {
            OpenVexDocumentVersion(3).next() shouldBe OpenVexDocumentVersion(4)
        }

        test("parses a version that has a successor") {
            OpenVexDocumentVersion.parse(1)?.value shouldBe 1
            OpenVexDocumentVersion.parse(Int.MAX_VALUE - 1L)?.value shouldBe Int.MAX_VALUE - 1
        }

        test("parses no version below 1 or without a successor") {
            listOf(0L, -1L, Int.MAX_VALUE.toLong(), Long.MAX_VALUE).forEach { value ->
                OpenVexDocumentVersion.parse(value).shouldBeNull()
            }
        }

        test("cannot be built below 1") {
            shouldThrow<IllegalArgumentException> { OpenVexDocumentVersion(0) }
        }
    })
