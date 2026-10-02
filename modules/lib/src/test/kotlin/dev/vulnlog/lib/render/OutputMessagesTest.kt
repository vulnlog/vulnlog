// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.model.OutputWrite
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OutputMessagesTest :
    FunSpec({

        test("renderWriteFailure names the target, why it cannot be written and what to do") {
            val failures =
                listOf(
                    OutputWrite.MissingDirectory("out/report.html"),
                    OutputWrite.Denied("out/report.html"),
                    OutputWrite.Unwritable("out/report.html", "Read-only file system"),
                )

            val rendered = failures.map(::renderWriteFailure)

            rendered shouldBe
                listOf(
                    Failure("cannot write out/report.html: its directory does not exist", "create the directory first"),
                    Failure(
                        "cannot write out/report.html: permission denied",
                        "make the location writable for this user",
                    ),
                    Failure("cannot write out/report.html: Read-only file system", "pass a writable file path"),
                )
        }
    })
