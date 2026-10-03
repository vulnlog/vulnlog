// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.finding.FormatFinding
import dev.vulnlog.lib.finding.FormatRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FormatMessagesTest :
    FunSpec({

        test("the comments-dropped warning names the file and the fields to use instead") {
            val warning = formatCommentsDroppedWarning("web-app.vl.yaml")

            warning shouldBe
                "warning: web-app.vl.yaml: contains YAML comments; they are removed on write\n" +
                "  hint: record notes in schema fields (e.g. comment, analysis)"
        }

        test("renderFormatFinding tags a finding with its rule id and names the path when there is one") {
            val findings =
                listOf(
                    FormatFinding(FormatRule.NON_CANONICAL_ARRAY_STYLE, "vulnerabilities[CVE-2026-0001].releases", "m"),
                    FormatFinding(FormatRule.COMMENTS_NOT_PRESERVED, "", "m"),
                )

            val rendered = findings.map(::renderFormatFinding)

            rendered shouldBe
                listOf(
                    "[non-canonical-array-style] vulnerabilities[CVE-2026-0001].releases: m",
                    "[comments-not-preserved] m",
                )
        }
    })
