// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.app.report.ImpactReportFormat.HTML
import dev.vulnlog.lib.app.report.ImpactReportFormat.JSON
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ImpactReportFormatTest :
    FunSpec({

        test("fromToken parses each token in any case, and each format names its file extension") {
            val tokens = listOf("html", "json", "HTML", "Json")

            val formats = tokens.map(ImpactReportFormat::fromToken)

            formats shouldBe listOf(HTML, JSON, HTML, JSON)
            formats.map { it.fileExtension } shouldBe listOf("html", "json", "html", "json")
        }

        test("fromToken rejects an unknown token and lists the valid ones") {
            val failure = shouldThrow<IllegalArgumentException> { ImpactReportFormat.fromToken("yaml") }

            failure.message shouldBe "Unknown impact report format 'yaml'. Valid values: html, json."
        }
    })
