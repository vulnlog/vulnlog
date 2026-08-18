// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import java.nio.file.Path

/** The text of one input. Everything downstream works on this rather than on a path, keeping the pipeline free of I/O */
data class InputDocument(
    val content: String,
    val filename: String,
    val path: Path? = null,
) {
    init {
        require(content.isNotBlank()) { "content must not be blank" }
        require(filename.isNotBlank()) { "filename must not be blank" }
    }

    /**
     * How the input is addressed on the command line: the full path for a file, the synthetic `<stdin>` name otherwise.
     * Findings about the file as a whole use this, findings inside the document use [filename].
     */
    val source: String get() = path?.toString() ?: filename
}
