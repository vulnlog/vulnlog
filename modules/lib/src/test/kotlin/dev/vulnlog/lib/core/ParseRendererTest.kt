// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.result.ParseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ParseRendererTest :
    FunSpec({

        test("renders the failure with its location") {
            val failure = ParseResult.Error(listOf(ParseFailure("boom", location = FailureLocation(3, 5))))

            renderParseFailure("a.vl.yaml", failure) shouldBe listOf("error: a.vl.yaml: 3:5: boom")
        }

        test("renders the failure without a position when the location is unknown") {
            val failure = ParseResult.Error(listOf(ParseFailure("boom")))

            renderParseFailure("a.vl.yaml", failure) shouldBe listOf("error: a.vl.yaml: boom")
        }

        test("renders the entry path between position and message") {
            val failure =
                ParseResult.Error(
                    listOf(ParseFailure("boom", "vulnerabilities[CVE-1].id", FailureLocation(3, 5))),
                )

            renderParseFailure("a.vl.yaml", failure) shouldBe
                listOf("error: a.vl.yaml: 3:5: vulnerabilities[CVE-1].id: boom")
        }

        test("renders every failure on its own line") {
            val failure =
                ParseResult.Error(
                    listOf(
                        ParseFailure("boom", "vulnerabilities[CVE-1].id", FailureLocation(3, 5)),
                        ParseFailure("bang", "vulnerabilities[CVE-2].verdict"),
                    ),
                )

            renderParseFailure("a.vl.yaml", failure) shouldBe
                listOf(
                    "error: a.vl.yaml: 3:5: vulnerabilities[CVE-1].id: boom",
                    "error: a.vl.yaml: vulnerabilities[CVE-2].verdict: bang",
                )
        }
    })
