// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.document.InputRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InputMessagesTest :
    FunSpec({

        test("renderInputFailure names the input, why it cannot be read and what to do") {
            val failures =
                listOf(
                    InputRead.Missing("app.vl.yaml"),
                    InputRead.Denied("app.vl.yaml"),
                    InputRead.Unreadable("app.vl.yaml", "Is a directory"),
                )

            val rendered = failures.map(::renderInputFailure)

            rendered shouldBe
                listOf(
                    Failure("cannot read app.vl.yaml: it does not exist", "check the path"),
                    Failure("cannot read app.vl.yaml: permission denied", "make the file readable for this user"),
                    Failure("cannot read app.vl.yaml: Is a directory", "pass a readable Vulnlog file"),
                )
        }
    })
