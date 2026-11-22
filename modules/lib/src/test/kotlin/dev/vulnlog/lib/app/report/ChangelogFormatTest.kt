// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.app.report.ChangelogFormat.MARKDOWN
import dev.vulnlog.lib.app.report.ChangelogFormat.TEXT
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChangelogFormatTest :
    FunSpec({

        test("fromToken parses each token in any case, and each format names its file extension") {
            val tokens = listOf("text", "markdown", "TEXT", "Markdown")

            val formats = tokens.map(ChangelogFormat::fromToken)

            formats shouldBe listOf(TEXT, MARKDOWN, TEXT, MARKDOWN)
            formats.map { it.fileExtension } shouldBe listOf("txt", "md", "txt", "md")
        }

        test("fromToken rejects an unknown token and lists the valid ones") {
            val failure = shouldThrow<IllegalArgumentException> { ChangelogFormat.fromToken("html") }

            failure.message shouldBe "Unknown changelog format 'html'. Valid values: text, markdown."
        }
    })
