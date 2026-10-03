// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.AddOutcome
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val DOCUMENT = InputDocument("---\n", "x.vl.yaml")
private val ID = VulnId.Cve("CVE-2026-1234")

class AddMessagesTest :
    FunSpec({

        test("renderAddStatus names the vulnerability and whether it was added or updated") {
            val outcomes =
                listOf(AddOutcome.Added(DOCUMENT, ID, "", false), AddOutcome.Updated(DOCUMENT, ID, "", false))

            val messages = outcomes.map(::renderAddStatus)

            messages shouldContainExactly
                listOf(
                    Message.Status("Added: CVE-2026-1234 to x.vl.yaml"),
                    Message.Status("Updated: CVE-2026-1234 in x.vl.yaml"),
                )
        }

        test("renderAddMessages warns only when the rewrite drops comments") {
            val outcomes =
                listOf(AddOutcome.Added(DOCUMENT, ID, "", true), AddOutcome.Updated(DOCUMENT, ID, "", false))

            val messages = outcomes.map(::renderAddMessages)

            messages.first() shouldContainExactly listOf(renderCommentsDropped("x.vl.yaml"))
            messages.last().shouldBeEmpty()
        }

        test("renderAddFailure names the unknown releases and tags, each with what to do") {
            val failed = AddOutcome.UnknownReferences(DOCUMENT, listOf(Release("9.9.9")), listOf(Tag("a"), Tag("b")))

            val failures = renderAddFailure(failed)

            failures shouldBe
                listOf(
                    Failure(
                        "x.vl.yaml: releases not defined in the file: 9.9.9",
                        "declare them under 'releases' in the file first",
                    ),
                    Failure(
                        "x.vl.yaml: tags not defined in the file: a, b",
                        "declare them under 'tags' in the file first",
                    ),
                )
        }
    })
