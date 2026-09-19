// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import dev.vulnlog.lib.model.Project
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage

class OpenVexAuthorTest :
    FunSpec({

        test("appends the contact in parentheses") {
            val project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@acme.example")

            OpenVexAuthor(project).name shouldBe "Acme Security Team (security@acme.example)"
        }

        test("is the author alone without a contact") {
            val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")

            OpenVexAuthor(project).name shouldBe "Acme Security Team"
        }

        listOf("", " ", " ".repeat(4), "\t", "\n").forEach {
            test("throw when author is '$it'") {
                val project = Project("Acme Corp", "Acme Web App", it)

                shouldThrow<IllegalArgumentException> {
                    OpenVexAuthor(project) shouldBe "Acme Security Team"
                }.shouldHaveMessage("VEX author is required")
            }
        }
    })
