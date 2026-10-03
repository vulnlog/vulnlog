// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.CopiedFile
import dev.vulnlog.lib.app.CopyOutcome
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val TARGET = InputDocument("---\n", "x.vl.yaml")
private val CVE = VulnId.Cve("CVE-2026-1234")
private val GHSA = VulnId.Ghsa("GHSA-1234-5678-abcd")

class CopyMessagesTest :
    FunSpec({

        test("renderCopied lists the copied ids verbosely and counts them in the status") {
            val files = listOf(listOf(CVE), listOf(CVE, GHSA)).map { ids -> CopiedFile(TARGET, ids, "", false) }

            val messages = files.map(::renderCopied)

            messages shouldContainExactly
                listOf(
                    listOf(
                        Message.Verbose("copied to x.vl.yaml: CVE-2026-1234"),
                        Message.Status("Copied: 1 entry to x.vl.yaml"),
                    ),
                    listOf(
                        Message.Verbose("copied to x.vl.yaml: CVE-2026-1234, GHSA-1234-5678-abcd"),
                        Message.Status("Copied: 2 entries to x.vl.yaml"),
                    ),
                )
        }

        test("renderCopyMessages warns only when the rewrite drops comments") {
            val files = listOf(CopiedFile(TARGET, listOf(CVE), "", true), CopiedFile(TARGET, listOf(CVE), "", false))

            val messages = files.map(::renderCopyMessages)

            messages.first() shouldContainExactly listOf(renderCommentsDropped("x.vl.yaml"))
            messages.last().shouldBeEmpty()
        }

        test("renderCopyFailure names the source and the ids it lacks") {
            val failed = CopyOutcome.IdsNotInSource(InputDocument("---\n", "source.vl.yaml"), listOf(CVE, GHSA))

            val failures = renderCopyFailure(failed)

            failures shouldBe
                listOf(
                    Failure(
                        "vulnerability IDs not found in source.vl.yaml: CVE-2026-1234, GHSA-1234-5678-abcd",
                        "copy only IDs the source file records",
                    ),
                )
        }
    })
