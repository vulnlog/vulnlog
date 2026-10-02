// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChangelogFormatRequestTest :
    FunSpec({

        test("fromToken parses each token in any case, and each format names its file extension") {
            val tokens = listOf("text", "markdown", "TEXT", "Markdown")

            val requests = tokens.map(ChangelogFormatRequest::fromToken)

            ChangelogFormatRequest.byToken.keys shouldBe setOf("text", "markdown")
            requests shouldBe
                listOf(
                    ChangelogFormatRequest.Text,
                    ChangelogFormatRequest.Markdown,
                    ChangelogFormatRequest.Text,
                    ChangelogFormatRequest.Markdown,
                )
            requests.map { it.fileExtension } shouldBe listOf("txt", "md", "txt", "md")
        }

        test("fromToken rejects an unknown token and lists the valid ones") {
            val failure = shouldThrow<IllegalArgumentException> { ChangelogFormatRequest.fromToken("html") }

            failure.message shouldBe "Unknown changelog format 'html'. Valid values: text, markdown."
        }
    })
