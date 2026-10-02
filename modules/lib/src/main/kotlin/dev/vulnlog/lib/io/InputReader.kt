// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.InputRead
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.readText

private const val STDIN = "<stdin>"

fun readInputDocument(input: FileInputOption): InputRead =
    when (input) {
        is FileInputOption.File -> readFile(input.path)
        FileInputOption.Stdin -> readStdin()
    }

private fun readFile(path: Path): InputRead {
    val source = path.toString()
    val text =
        try {
            path.readText()
        } catch (_: NoSuchFileException) {
            return InputRead.Missing(source)
        } catch (_: AccessDeniedException) {
            return InputRead.Denied(source)
        } catch (e: IOException) {
            return InputRead.Unreadable(source, e.reason())
        }
    return InputRead.Read(InputDocument(text, path.name, path))
}

private fun readStdin(): InputRead {
    val text =
        try {
            System.`in`.bufferedReader().readText()
        } catch (e: IOException) {
            return InputRead.Unreadable(STDIN, e.reason())
        }
    return InputRead.Read(InputDocument(text, STDIN))
}
