// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FormatOutcome
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.finding.FormatFinding
import dev.vulnlog.lib.finding.FormatRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val DOCUMENT = InputDocument("---\n", "web-app.vl.yaml")
private val FINDINGS =
    listOf(
        FormatFinding(FormatRule.NON_CANONICAL_ARRAY_STYLE, "vulnerabilities[CVE-2026-0001].releases", "m"),
        FormatFinding(FormatRule.COMMENTS_NOT_PRESERVED, "", "n"),
    )

class FormatMessagesTest :
    FunSpec({

        test("a rewrite warns that comments go and lists what it fixes at debug, tagged with the rule id") {
            val outcome = FormatOutcome.Reformatted(DOCUMENT, "", FINDINGS, commentsDropped = true)

            val messages = renderFormatMessages(outcome)

            messages shouldContainExactly
                listOf(
                    renderCommentsDropped("web-app.vl.yaml"),
                    Message.Debug("[non-canonical-array-style] vulnerabilities[CVE-2026-0001].releases: m"),
                    Message.Debug("[comments-not-preserved] n"),
                )
        }

        test("a rewrite without comments warns of nothing") {
            val outcome = FormatOutcome.Reformatted(DOCUMENT, "", emptyList(), commentsDropped = false)

            val messages = renderFormatMessages(outcome)

            messages.shouldBeEmpty()
        }

        test("a file that is not canonical carries its findings in its warning") {
            val outcome = FormatOutcome.NotCanonical(DOCUMENT, FINDINGS)

            val messages = renderFormatMessages(outcome)

            messages shouldContainExactly
                listOf(
                    Message.Warning(
                        "web-app.vl.yaml: not canonically formatted\n" +
                            "  [non-canonical-array-style] vulnerabilities[CVE-2026-0001].releases: m\n" +
                            "  [comments-not-preserved] n",
                    ),
                )
        }

        test("an unchanged file says nothing; the driver words its status") {
            val messages = renderFormatMessages(FormatOutcome.Unchanged(DOCUMENT))

            messages.shouldBeEmpty()
        }

        test("the comments-dropped warning names the file and the fields to use instead") {
            val warning = renderCommentsDropped("web-app.vl.yaml")

            warning shouldBe
                Message.Warning(
                    "web-app.vl.yaml: contains YAML comments; they are removed on write\n" +
                        "  hint: record notes in schema fields (e.g. comment, analysis)",
                )
        }

        test("renderNotFormatted names every file and the driver's way to fix them") {
            val failure = renderNotFormatted(listOf("a.vl.yaml", "b.vl.yaml"), "run the vulnlogFormat task to fix them")

            failure shouldBe
                Failure(
                    "some Vulnlog files are not formatted: a.vl.yaml, b.vl.yaml",
                    "run the vulnlogFormat task to fix them",
                )
        }
    })
