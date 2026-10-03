// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.copyVulnerabilitiesToFile
import dev.vulnlog.lib.document.validated
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.fixtures.vulnlogDocument
import dev.vulnlog.lib.model.VulnId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val CVE = VulnId.Cve("CVE-2026-1234")
private val UNKNOWN = VulnId.Cve("CVE-2026-0000")

/** Records CVE-2026-1234. */
private val SOURCE = validated(vulnlogDocument())
private val TARGET = validated(vulnlogDocument(vulnId = "CVE-2026-5678"))

private fun editOf(destination: ValidVulnlogProject): String =
    copyVulnerabilitiesToFile(SOURCE.vulnlogProjectFile, destination, setOf(CVE))

class CopyVulnerabilitiesTest :
    FunSpec({

        test("copies into every destination, each with its own edit and comments flag") {
            val commented = validated(vulnlogDocument(vulnId = "CVE-2026-5678").replaceFirst("---\n", "---\n# notes\n"))
            val destinations = listOf(TARGET, commented)

            val outcome = copyVulnerabilities(SOURCE, destinations, CopyRequest(setOf(CVE)))

            val files = outcome.shouldBeInstanceOf<CopyOutcome.Copied>().files
            files.map { it.document } shouldContainExactly destinations.map { it.inputDocument }
            files.map { it.ids } shouldContainExactly List(2) { listOf(CVE) }
            files.map { it.content } shouldContainExactly destinations.map(::editOf)
            files.map { it.commentsDropped } shouldContainExactly listOf(false, true)
        }

        test("refuses ids the source does not record, before it copies anything") {
            val request = CopyRequest(setOf(CVE, UNKNOWN))

            val outcome = copyVulnerabilities(SOURCE, listOf(TARGET), request)

            outcome shouldBe CopyOutcome.IdsNotInSource(SOURCE.inputDocument, listOf(UNKNOWN))
        }
    })
