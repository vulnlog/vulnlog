// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class FailureTest :
    FunSpec({

        test("formatFailureLines follows every error with its hint, in order") {
            val failures =
                listOf(
                    Failure("no statement applies", "declare 'purls' on the releases"),
                    Failure("Tag not found: binary", "Known tags: app"),
                )

            val lines = formatFailureLines(failures)

            lines shouldContainExactly
                listOf(
                    "error: no statement applies",
                    "  hint: declare 'purls' on the releases",
                    "error: Tag not found: binary",
                    "  hint: Known tags: app",
                )
        }

        test("formatFailureMessage makes a sentence of every message and hint, without doubling a period") {
            val failures =
                listOf(
                    Failure(
                        "baseline 'vex.json' is not an OpenVEX document",
                        "omit 'baseline' to issue a new document",
                    ),
                    Failure("Tag not found: binary", "The input declares no tags."),
                )

            val message = formatFailureMessage(failures)

            message shouldBe
                "Baseline 'vex.json' is not an OpenVEX document. Omit 'baseline' to issue a new document. " +
                "Tag not found: binary. The input declares no tags."
        }
    })
