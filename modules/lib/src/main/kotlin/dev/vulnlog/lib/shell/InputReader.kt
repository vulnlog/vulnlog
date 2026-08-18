// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import java.io.IOException
import kotlin.io.path.name
import kotlin.io.path.readText

fun readInputDocument(input: FileInputOption): InputDocument {
    try {
        return when (input) {
            is FileInputOption.File -> InputDocument(input.path.readText(), input.path.name, input.path)
            FileInputOption.Stdin -> InputDocument(System.`in`.bufferedReader().readText(), "<stdin>")
        }
    } catch (e: IOException) {
        error("Cannot read ${input.sourceFile().name}: ${e.message}")
    }
}
