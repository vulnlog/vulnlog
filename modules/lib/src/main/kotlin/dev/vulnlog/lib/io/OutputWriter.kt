// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.OutputWrite
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun writeOutput(
    path: Path,
    text: String,
    createDirectories: Boolean = false,
): OutputWrite =
    attempt(path) {
        write(path, text, createDirectories)
        OutputWrite.Written
    }

/** Leaves an identical file untouched, so a committed file keeps its timestamp. */
fun writeOutputIfChanged(
    path: Path,
    text: String,
    createDirectories: Boolean = false,
): OutputWrite =
    attempt(path) {
        if (path.isRegularFile() && path.readText() == text) {
            OutputWrite.Unchanged
        } else {
            write(path, text, createDirectories)
            OutputWrite.Written
        }
    }

private fun write(
    path: Path,
    text: String,
    createDirectories: Boolean,
) {
    if (createDirectories) path.parent?.createDirectories()
    path.writeText(text)
}

private fun attempt(
    path: Path,
    block: () -> OutputWrite,
): OutputWrite {
    val target = path.toString()
    return try {
        block()
    } catch (_: NoSuchFileException) {
        OutputWrite.MissingDirectory(target)
    } catch (_: AccessDeniedException) {
        OutputWrite.Denied(target)
    } catch (e: IOException) {
        OutputWrite.Unwritable(target, e.reason())
    }
}
