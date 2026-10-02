// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.fixtures.withStdin
import dev.vulnlog.lib.fixtures.withTempFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.writeText

private const val CONTENT = "schemaVersion: \"1\"\n"

/** False where permissions cannot take read access away: no POSIX file system, or a user such as root that ignores them. */
private fun canDenyRead(): Boolean {
    val file = Files.createTempFile("vulnlog", ".vl.yaml")
    return try {
        Files.setPosixFilePermissions(file, emptySet())
        !Files.isReadable(file)
    } catch (_: UnsupportedOperationException) {
        false
    } finally {
        Files.deleteIfExists(file)
    }
}

class InputReaderTest :
    FunSpec({

        test("reads a file with its name, and addresses it by its full path") {
            withTempFile(content = CONTENT) { file ->
                val read = readInputDocument(FileInputOption.File(file))

                val input = read.shouldBeInstanceOf<InputRead.Read>().document
                input.content shouldBe CONTENT
                input.filename shouldBe file.fileName.toString()
                input.path shouldBe file
                input.source shouldBe file.toString()
            }
        }

        test("reads stdin under the synthetic name <stdin>, without a path") {
            withStdin(CONTENT) {
                val read = readInputDocument(FileInputOption.Stdin)

                val input = read.shouldBeInstanceOf<InputRead.Read>().document
                input.content shouldBe CONTENT
                input.filename shouldBe "<stdin>"
                input.path shouldBe null
                input.source shouldBe "<stdin>"
            }
        }

        test("reports a missing file as missing") {
            val missing = tempdir().toPath().resolve("absent.vl.yaml")

            val read = readInputDocument(FileInputOption.File(missing))

            read shouldBe InputRead.Missing(missing.toString())
        }

        test("reads blank content as it is, so that parsing judges it like an empty YAML document") {
            val blank = tempdir().toPath().resolve("blank.vl.yaml").also { it.writeText("  \n") }

            val fileRead = readInputDocument(FileInputOption.File(blank))
            val stdinRead = withStdin("") { readInputDocument(FileInputOption.Stdin) }

            val contents = listOf(fileRead, stdinRead).map { it.shouldBeInstanceOf<InputRead.Read>().document.content }
            contents shouldBe listOf("  \n", "")
        }

        test("reports a directory as unreadable, in the file system's own words") {
            val directory: Path = tempdir().toPath()

            val read = readInputDocument(FileInputOption.File(directory))

            val unreadable = read.shouldBeInstanceOf<InputRead.Unreadable>()
            unreadable.source shouldBe directory.toString()
            unreadable.reason.shouldNotBeBlank()
        }

        test("reports a file without read permission as denied").config(enabledIf = { canDenyRead() }) {
            val file = tempdir().toPath().resolve("locked.vl.yaml").also { it.writeText(CONTENT) }
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("-w-------"))

            val read = readInputDocument(FileInputOption.File(file))

            read shouldBe InputRead.Denied(file.toString())
        }
    })
