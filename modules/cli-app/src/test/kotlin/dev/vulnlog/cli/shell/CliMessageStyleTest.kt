// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CliMessageStyleTest :
    FunSpec({

        test("error prefix renders red and bold") {
            styleMessage("error: vulnlog.yaml: boom") shouldBe
                (TextColors.red + TextStyles.bold)("error:") + " vulnlog.yaml: boom"
        }

        test("warning prefix renders yellow") {
            styleMessage("warning: vulnlog.yaml: not canonically formatted") shouldBe
                TextColors.yellow("warning:") + " vulnlog.yaml: not canonically formatted"
        }

        test("hint prefix renders cyan and keeps the indent") {
            styleMessage("  hint: run vulnlog fmt") shouldBe
                "  " + TextColors.cyan("hint:") + " run vulnlog fmt"
        }

        test("status verbs render green") {
            styleMessage("Created: vulnlog.yaml") shouldBe
                TextColors.green("Created") + ": vulnlog.yaml"
            styleMessage("Wrote: reports/report.html") shouldBe
                TextColors.green("Wrote") + ": reports/report.html"
            styleMessage("Validated: vulnlog.yaml") shouldBe
                TextColors.green("Validated") + ": vulnlog.yaml"
        }

        test("summary counts render bold") {
            styleMessage("2 errors, 1 warning") shouldBe
                TextStyles.bold("2") + " errors, " + TextStyles.bold("1") + " warning"
        }

        test("info findings and plain lines stay unstyled") {
            styleMessage("info: vulnlog.yaml: some observation") shouldBe "info: vulnlog.yaml: some observation"
            styleMessage("some plain line") shouldBe "some plain line"
        }

        test("multi-line messages style each line on its own") {
            styleMessage("warning: f.yaml: contains YAML comments\n  hint: record notes in schema fields") shouldBe
                TextColors.yellow("warning:") + " f.yaml: contains YAML comments\n" +
                "  " + TextColors.cyan("hint:") + " record notes in schema fields"
        }

        test("a subject containing a verb-like word is not a status line") {
            styleMessage("Copied files are large") shouldBe "Copied files are large"
        }
    })
