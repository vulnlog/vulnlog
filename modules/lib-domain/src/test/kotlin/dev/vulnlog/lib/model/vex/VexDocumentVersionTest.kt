// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class VexDocumentVersionTest :
    FunSpec({

        test("the next version is one more") {
            val version = VexDocumentVersion(3)

            val next = version.next()

            next shouldBe VexDocumentVersion(4)
        }

        test("parses a version that has a successor") {
            val values = listOf(1L, Int.MAX_VALUE - 1L)

            val versions = values.map(VexDocumentVersion::parse)

            versions.map { it?.value } shouldBe listOf(1, Int.MAX_VALUE - 1)
        }

        test("parses no version below 1 or without a successor") {
            val values = listOf(0L, -1L, Int.MAX_VALUE.toLong(), Long.MAX_VALUE)

            val versions = values.map(VexDocumentVersion::parse)

            versions.forEach { it.shouldBeNull() }
        }

        test("cannot be built below 1") {
            shouldThrow<IllegalArgumentException> { VexDocumentVersion(0) }
        }
    })
