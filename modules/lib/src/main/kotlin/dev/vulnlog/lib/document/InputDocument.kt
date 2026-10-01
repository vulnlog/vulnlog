// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

import java.nio.file.Path

/** Downstream code works on the text, not on a path, so the pipeline stays free of I/O. */
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
     * The full path for a file, `<stdin>` otherwise. Findings about the whole file name this, findings inside it
     * [filename].
     */
    val source: String get() = path?.toString() ?: filename
}
