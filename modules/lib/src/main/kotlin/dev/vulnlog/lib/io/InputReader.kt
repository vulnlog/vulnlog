// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.document.InputDocument
import java.io.IOException
import kotlin.io.path.name
import kotlin.io.path.readText

fun readInputDocument(input: FileInputOption): InputDocument {
    val name =
        when (input) {
            is FileInputOption.File -> input.path.name
            FileInputOption.Stdin -> "<stdin>"
        }
    try {
        return when (input) {
            is FileInputOption.File -> InputDocument(input.path.readText(), name, input.path)
            FileInputOption.Stdin -> InputDocument(System.`in`.bufferedReader().readText(), name)
        }
    } catch (e: IOException) {
        error("Cannot read $name: ${e.message}")
    }
}
