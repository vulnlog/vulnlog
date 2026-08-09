// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.fixtures.withStdin
import dev.vulnlog.lib.fixtures.withTempFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path

private const val CONTENT = "schemaVersion: \"1\"\n"

class InputReaderTest :
    FunSpec({

        context("a file input") {

            test("is read with its content, name and path") {
                withTempFile(content = CONTENT) { file ->
                    val input = readInputDocument(FileInputOption.File(file))

                    input.content shouldBe CONTENT
                    input.filename shouldBe file.fileName.toString()
                    input.path shouldBe file
                }
            }

            test("is addressed by its full path") {
                withTempFile(content = CONTENT) { file ->
                    val input = readInputDocument(FileInputOption.File(file))

                    input.source shouldBe file.toString()
                }
            }

            test("that cannot be read reports the file name") {
                val missing = Path.of("/nonexistent/vulnlog.vl.yaml")

                val error = shouldThrow<IllegalStateException> { readInputDocument(FileInputOption.File(missing)) }

                error.message shouldContain "Cannot read vulnlog.vl.yaml"
            }
        }

        context("a stdin input") {

            test("is read with the synthetic file name and no path") {
                withStdin(CONTENT) {
                    val input = readInputDocument(FileInputOption.Stdin)

                    input.content shouldBe CONTENT
                    input.filename shouldBe "<stdin>"
                    input.path shouldBe null
                }
            }

            test("is addressed by the synthetic file name") {
                withStdin(CONTENT) {
                    val input = readInputDocument(FileInputOption.Stdin)

                    input.source shouldBe "<stdin>"
                }
            }
        }
    })
