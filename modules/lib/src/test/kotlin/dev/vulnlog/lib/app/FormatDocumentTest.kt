// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.checkFormat
import dev.vulnlog.lib.document.formatYaml
import dev.vulnlog.lib.document.parsed
import dev.vulnlog.lib.finding.FormatRule
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

private val CANONICAL = formatYaml(parsed(vulnlogDocument()))
private val COMMENTED = CANONICAL.replaceFirst("---\n", "---\n# reviewed by the security team\n")

class FormatDocumentTest :
    FunSpec({

        test("leaves canonical content unchanged, in both modes") {
            val project = parsed(CANONICAL)

            val outcomes = listOf(false, true).map { check -> formatDocument(project, FormatRequest(check)) }

            outcomes shouldBe List(2) { FormatOutcome.Unchanged(project.inputDocument) }
        }

        test("rewrites other content, with the findings it fixes and whether it drops comments") {
            val project = parsed(COMMENTED)

            val outcome = formatDocument(project, FormatRequest(check = false))

            outcome shouldBe
                FormatOutcome.Reformatted(
                    document = project.inputDocument,
                    formatted = CANONICAL,
                    findings = checkFormat(project),
                    commentsDropped = true,
                )
            checkFormat(project).map { it.rule } shouldContain FormatRule.COMMENTS_NOT_PRESERVED
        }

        test("in check mode reports the findings instead of rewriting") {
            val project = parsed(COMMENTED)

            val outcome = formatDocument(project, FormatRequest(check = true))

            outcome shouldBe FormatOutcome.NotCanonical(project.inputDocument, checkFormat(project))
        }
    })
