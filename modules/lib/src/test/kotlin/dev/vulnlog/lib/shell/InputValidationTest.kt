// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.shell.InputSelectionResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private fun file(name: String) = FileInputOption.File(Path.of(name))

class InputValidationTest :
    FunSpec({

        context("validateInputSelection") {
            test("a single file is allowed") {
                validateInputSelection(listOf(file("vulnlog.yaml"))) shouldBe InputSelectionResult.Ok
            }

            test("multiple files are allowed") {
                validateInputSelection(listOf(file("a.vl.yaml"), file("b.vl.yaml"))) shouldBe InputSelectionResult.Ok
            }

            test("a single stdin is allowed") {
                validateInputSelection(listOf(FileInputOption.Stdin)) shouldBe InputSelectionResult.Ok
            }

            test("more than one stdin is rejected") {
                validateInputSelection(listOf(FileInputOption.Stdin, FileInputOption.Stdin)) shouldBe
                    InputSelectionResult.Error("Multiple <stdin> are not supported.")
            }

            test("stdin mixed with a file is rejected") {
                validateInputSelection(listOf(FileInputOption.Stdin, file("vulnlog.yaml"))) shouldBe
                    InputSelectionResult.Error("Mixing input files with STDIN is not allowed.")
            }
        }
    })
