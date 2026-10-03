// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.InputDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private fun request(
    targetExists: Boolean = false,
    force: Boolean = false,
) = InitRequest("acme", "widget", "alice", targetExists, force)

class InitDocumentTest :
    FunSpec({

        test("creates a file holding only the project, with the schema header") {
            val outcome = initDocument(request())

            outcome.shouldBeInstanceOf<InitOutcome.Created>().content shouldBe
                """
                |# ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
                |---
                |schemaVersion: "1"
                |
                |project:
                |  organization: acme
                |  name: widget
                |  author: alice
                |
                |releases: []
                |
                |vulnerabilities: []
                |
                """.trimMargin()
        }

        test("the created file passes validation without a finding") {
            val created = initDocument(request()).shouldBeInstanceOf<InitOutcome.Created>()

            val outcome = validateDocument(InputDocument(created.content, "vulnlog.yaml"))

            outcome.shouldBeInstanceOf<ValidationOutcome.Ok<*>>().findings.shouldBeEmpty()
        }

        test("refuses an existing target unless forced") {
            val requests = listOf(request(targetExists = true), request(targetExists = true, force = true))

            val outcomes = requests.map(::initDocument)

            outcomes.first() shouldBe InitOutcome.AlreadyExists
            outcomes.last().shouldBeInstanceOf<InitOutcome.Created>()
        }
    })
