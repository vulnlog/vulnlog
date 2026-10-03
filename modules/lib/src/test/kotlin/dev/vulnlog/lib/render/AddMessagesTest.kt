// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.document.AddOutcome
import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class AddMessagesTest :
    FunSpec({

        test("formatAddOutcomeMessage names the vulnerability and whether it was added or updated") {
            val added = AddOutcome(newContent = "", vulnId = VulnId.Cve("CVE-2026-1234"), updated = false)
            val updated = added.copy(updated = true)

            val messages = listOf(added, updated).map { formatAddOutcomeMessage(Path.of("/tmp/x.vl.yaml"), it) }

            messages shouldBe
                listOf("Added: CVE-2026-1234 to /tmp/x.vl.yaml", "Updated: CVE-2026-1234 in /tmp/x.vl.yaml")
        }
    })
