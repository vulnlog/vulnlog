// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import java.io.File
import java.nio.file.Path

sealed interface FileInputOption {
    data object Stdin : FileInputOption

    data class File(
        val path: Path,
    ) : FileInputOption
}

/**
 * The [File] key an input is identified by in parse/validation results: the real path for a
 * [FileInputOption.File], a synthetic `<stdin>` for [FileInputOption.Stdin].
 */
fun FileInputOption.sourceFile(): File =
    when (this) {
        is FileInputOption.File -> path.toFile()
        FileInputOption.Stdin -> File("<stdin>")
    }
