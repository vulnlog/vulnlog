// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.OutputWrite
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectory
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText

/** False where permissions cannot take write access away: no POSIX file system, or a user such as root. */
private fun canDenyWrite(): Boolean {
    val directory = Files.createTempDirectory("vulnlog")
    return try {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"))
        !Files.isWritable(directory)
    } catch (_: UnsupportedOperationException) {
        false
    } finally {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"))
        Files.deleteIfExists(directory)
    }
}

class OutputWriterTest :
    FunSpec({

        test("writes a new file and replaces an existing one") {
            val directory = tempdir().toPath()
            val existing = directory.resolve("old.txt").also { it.writeText("old") }
            val fresh = directory.resolve("new.txt")

            val writes = listOf(writeOutput(existing, "replaced"), writeOutput(fresh, "created"))

            writes shouldBe listOf(OutputWrite.Written, OutputWrite.Written)
            listOf(existing.readText(), fresh.readText()) shouldBe listOf("replaced", "created")
        }

        test("creates missing directories only when asked, and names the target otherwise") {
            val directory = tempdir().toPath()
            val created = directory.resolve("a/b/created.txt")
            val missing = directory.resolve("c/d/missing.txt")

            val writes = listOf(writeOutput(created, "x", createDirectories = true), writeOutput(missing, "x"))

            writes shouldBe listOf(OutputWrite.Written, OutputWrite.MissingDirectory(missing.toString()))
        }

        test("writeOutputIfChanged leaves an identical file untouched and replaces a different one") {
            val directory = tempdir().toPath()
            val old = FileTime.fromMillis(0)
            val same =
                directory.resolve("same.txt").also {
                    it.writeText("text")
                    it.setLastModifiedTime(old)
                }
            val other = directory.resolve("other.txt").also { it.writeText("old") }

            val writes = listOf(writeOutputIfChanged(same, "text"), writeOutputIfChanged(other, "new"))

            writes shouldBe listOf(OutputWrite.Unchanged, OutputWrite.Written)
            same.getLastModifiedTime() shouldBe old
            other.readText() shouldBe "new"
        }

        test("reports a directory as unwritable, in the file system's words without repeating the path") {
            val directory: Path = tempdir().toPath().resolve("report.html").createDirectory()

            val write = writeOutput(directory, "x")

            val unwritable = write.shouldBeInstanceOf<OutputWrite.Unwritable>()
            unwritable.target shouldBe directory.toString()
            unwritable.reason.shouldNotBeBlank()
            unwritable.reason shouldNotContain directory.toString()
        }

        test("reports a directory without write permission as denied").config(enabledIf = { canDenyWrite() }) {
            val directory = tempdir().toPath()
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"))
            val target = directory.resolve("report.html")

            val write =
                try {
                    writeOutput(target, "x")
                } finally {
                    Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"))
                }

            write shouldBe OutputWrite.Denied(target.toString())
        }
    })
