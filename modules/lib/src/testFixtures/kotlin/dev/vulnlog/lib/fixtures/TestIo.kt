// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/** Creates a temporary Vulnlog file that is deleted after [block] returns. */
inline fun <R> withTempFile(
    prefix: String = "vulnlog",
    suffix: String = ".vl.yaml",
    content: String,
    block: (Path) -> R,
): R {
    val file = Files.createTempFile(prefix, suffix)
    return try {
        file.writeText(content)
        block(file)
    } finally {
        file.deleteIfExists()
    }
}

/** Replaces `System.in` with [content] for the duration of [block] and restores it afterwards. */
inline fun <R> withStdin(
    content: String,
    block: () -> R,
): R {
    val original = System.`in`
    return try {
        System.setIn(content.byteInputStream())
        block()
    } finally {
        System.setIn(original)
    }
}
