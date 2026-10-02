// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.createFile

private fun file(name: String) = FileInputOption.File(Path.of(name))

class InputValidationTest :
    FunSpec({

        test("isVulnlogFileName accepts vulnlog.yaml, vulnlog.yml, *.vl.yaml and *.vl.yml only") {
            val names = listOf("vulnlog.yaml", "vulnlog.yml", "acme.vl.yaml", "acme.vl.yml")
            val others = listOf("acme.yaml", "vulnlog.json", "acme.vl.yaml.bak", "vulnlog.yaml.vl")

            val accepted = (names + others).map(::isVulnlogFileName)

            accepted shouldBe List(names.size) { true } + List(others.size) { false }
        }

        test("validateInputPath accepts an existing Vulnlog file and names the problem otherwise") {
            val directory = tempdir().toPath()
            val valid = directory.resolve("acme.vl.yaml").createFile()
            val wrongName = directory.resolve("notes.yaml").createFile()
            val missing = directory.resolve("absent.vl.yaml")

            val results = listOf(valid, wrongName, missing).map(::validateInputPath)

            results shouldBe
                listOf(
                    InputValidationResult.Ok(valid),
                    InputValidationResult.Error("File name must be [vulnlog|*.vl].[yaml|yml]: $wrongName"),
                    InputValidationResult.Error("Path '$missing' does not exist."),
                )
        }

        test("validateInputSelection allows any number of files or a single stdin") {
            val selections =
                listOf(
                    listOf(file("vulnlog.yaml")),
                    listOf(file("a.vl.yaml"), file("b.vl.yaml")),
                    listOf(FileInputOption.Stdin),
                )

            val results = selections.map(::validateInputSelection)

            results shouldBe List(3) { InputSelectionResult.Ok }
        }

        test("validateInputSelection rejects a second stdin and stdin mixed with files") {
            val selections =
                listOf(
                    listOf(FileInputOption.Stdin, FileInputOption.Stdin),
                    listOf(FileInputOption.Stdin, file("vulnlog.yaml")),
                )

            val results = selections.map(::validateInputSelection)

            results shouldBe
                listOf(
                    InputSelectionResult.Error("Multiple <stdin> are not supported."),
                    InputSelectionResult.Error("Mixing input files with STDIN is not allowed."),
                )
        }
    })
