// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class OpenVexDocumentIdTest :
    FunSpec({

        test("parses an absolute IRI") {
            listOf("https://vulnlog.dev/vex/abc", "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79").forEach { value ->
                OpenVexDocumentId.parse(value)?.value shouldBe value
            }
        }

        test("parses nothing that is no absolute IRI") {
            listOf("", " ", "vex-1", "/vex/abc", " https://vulnlog.dev/vex/abc", "https://vulnlog.dev/a b").forEach {
                OpenVexDocumentId.parse(it).shouldBeNull()
            }
        }

        test("cannot be built from a value that is no absolute IRI") {
            shouldThrow<IllegalArgumentException> { OpenVexDocumentId("vex-1") }
        }
    })
