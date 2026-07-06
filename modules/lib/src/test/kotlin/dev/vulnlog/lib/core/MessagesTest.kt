// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.result.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MessagesTest :
    FunSpec({

        test("status line joins verb and subject with a colon") {
            formatStatus(StatusVerb.CREATED, "vulnlog.yaml") shouldBe "Created: vulnlog.yaml"
            formatStatus(StatusVerb.WROTE, "reports/report.html") shouldBe "Wrote: reports/report.html"
        }

        test("finding line includes file, location, and message") {
            formatFinding(
                severity = Severity.ERROR,
                file = "vulnlog.yaml",
                location = "vulnerabilities[3].resolution.in",
                message = "release '9.9.9' is not defined",
            ) shouldBe "error: vulnlog.yaml: vulnerabilities[3].resolution.in: release '9.9.9' is not defined"
        }

        test("finding line omits a missing location") {
            formatFinding(
                severity = Severity.WARNING,
                file = "vulnlog.yaml",
                message = "not canonically formatted",
            ) shouldBe "warning: vulnlog.yaml: not canonically formatted"
        }

        test("finding line omits a blank location") {
            formatFinding(
                severity = Severity.INFO,
                file = "vulnlog.yaml",
                location = "",
                message = "some observation",
            ) shouldBe "info: vulnlog.yaml: some observation"
        }

        test("message line carries only the severity prefix") {
            formatMessage(Severity.ERROR, "cannot read <stdin>") shouldBe "error: cannot read <stdin>"
        }

        test("hint line is indented under the finding") {
            formatHint("run vulnlog fmt") shouldBe "  hint: run vulnlog fmt"
        }

        test("summary uses real plurals") {
            formatSummary(errors = 2, warnings = 1) shouldBe "2 errors, 1 warning"
        }

        test("summary omits zero counts") {
            formatSummary(errors = 1, warnings = 0, infos = 3) shouldBe "1 error, 3 infos"
        }

        test("pluralize supports irregular plurals") {
            pluralize(1, "entry", "entries") shouldBe "1 entry"
            pluralize(3, "entry", "entries") shouldBe "3 entries"
        }

        test("pluralize drops zero counts") {
            pluralize(0, "error").shouldBeNull()
        }
    })
