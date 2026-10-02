// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.fixtures.withStdin
import dev.vulnlog.lib.fixtures.withTempFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.Path

private const val CONTENT = "schemaVersion: \"1\"\n"

class InputReaderTest :
    FunSpec({

        test("reads a file with its name, and addresses it by its full path") {
            withTempFile(content = CONTENT) { file ->
                val input = readInputDocument(FileInputOption.File(file))

                input.content shouldBe CONTENT
                input.filename shouldBe file.fileName.toString()
                input.path shouldBe file
                input.source shouldBe file.toString()
            }
        }

        test("reads stdin under the synthetic name <stdin>, without a path") {
            withStdin(CONTENT) {
                val input = readInputDocument(FileInputOption.Stdin)

                input.content shouldBe CONTENT
                input.filename shouldBe "<stdin>"
                input.path shouldBe null
                input.source shouldBe "<stdin>"
            }
        }

        test("names the file it cannot read") {
            val missing = Path.of("/nonexistent/vulnlog.vl.yaml")

            val error = shouldThrow<IllegalStateException> { readInputDocument(FileInputOption.File(missing)) }

            error.message shouldStartWith "Cannot read vulnlog.vl.yaml: "
        }
    })
