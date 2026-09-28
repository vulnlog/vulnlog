// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage

class OpenVexAuthorTest :
    FunSpec({

        test("holds the name and the contact apart") {
            val author = OpenVexAuthor("Acme Security Team", "security@acme.example")

            author.name shouldBe "Acme Security Team"
            author.contact shouldBe "security@acme.example"
        }

        listOf("", " ", " ".repeat(4), "\t", "\n").forEach {
            test("throw when the name is '$it'") {
                shouldThrow<IllegalArgumentException> { OpenVexAuthor(it) }.shouldHaveMessage("VEX author is required")
            }
        }

        test("throw when the contact is blank") {
            shouldThrow<IllegalArgumentException> {
                OpenVexAuthor("Acme Security Team", " ")
            }.shouldHaveMessage("VEX author contact must not be blank")
        }
    })
