// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.finding.FindingSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MessageGrammarTest :
    FunSpec({

        test("a status line joins verb and subject with a colon") {
            val line = formatStatus(StatusVerb.CREATED, "vulnlog.yaml")

            line shouldBe "Created: vulnlog.yaml"
        }

        test("a finding line names the severity, the file, the location and the message") {
            val location = "vulnerabilities[3].resolution.in"

            val line = formatFinding(FindingSeverity.ERROR, "vulnlog.yaml", location, "release '9.9.9' is not defined")

            line shouldBe "error: vulnlog.yaml: vulnerabilities[3].resolution.in: release '9.9.9' is not defined"
        }

        test("a finding line leaves out a missing or blank location") {
            val locations = listOf(null, "")

            val lines = locations.map { formatFinding(FindingSeverity.WARNING, "vulnlog.yaml", it, "not canonical") }

            lines shouldBe List(2) { "warning: vulnlog.yaml: not canonical" }
        }

        test("a message line carries only the severity prefix") {
            val line = formatMessage(FindingSeverity.INFO, "cannot read <stdin>")

            line shouldBe "info: cannot read <stdin>"
        }

        test("a hint line is indented under the line it follows") {
            val line = formatHint("run vulnlog fmt")

            line shouldBe "  hint: run vulnlog fmt"
        }

        test("a summary counts with real plurals and leaves out zero counts") {
            val counts = listOf(Triple(2, 1, 0), Triple(1, 0, 3), Triple(0, 0, 0))

            val summaries = counts.map { (errors, warnings, infos) -> formatSummary(errors, warnings, infos) }

            summaries shouldBe listOf("2 errors, 1 warning", "1 error, 3 infos", "")
        }

        test("pluralize takes an irregular plural and counts zero too") {
            val counts = listOf(0, 1, 3)

            val counted = counts.map { pluralize(it, "entry", "entries") }

            counted shouldBe listOf("0 entries", "1 entry", "3 entries")
        }
    })
