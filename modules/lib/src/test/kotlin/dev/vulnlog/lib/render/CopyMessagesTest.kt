// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private val cve1 = VulnId.Cve("CVE-2026-1234")
private val cve2 = VulnId.Cve("CVE-2026-5678")
private val ghsa1 = VulnId.Ghsa("GHSA-1234-5678-abcd")

class CopyMessagesTest :
    FunSpec({

        test("formatCopiedMessage counts the copied entries, or reports the destination unchanged") {
            val copied = listOf(listOf(cve1), listOf(cve1, cve2), emptyList())

            val messages = copied.map { formatCopiedMessage(Path.of("/tmp/x.vl.yaml"), it) }

            messages shouldBe
                listOf(
                    "Copied: 1 entry to /tmp/x.vl.yaml",
                    "Copied: 2 entries to /tmp/x.vl.yaml",
                    "Unchanged: /tmp/x.vl.yaml: no new vulnerabilities",
                )
        }

        test("formatVulnIdsNotInSourceMessage lists the missing ids as an error") {
            val missing = setOf(cve1, ghsa1)

            val message = formatVulnIdsNotInSourceMessage(missing)

            message shouldBe "error: vulnerability IDs not found in source file: CVE-2026-1234, GHSA-1234-5678-abcd"
        }
    })
