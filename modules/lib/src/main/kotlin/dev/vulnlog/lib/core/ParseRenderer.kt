// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.validation.ParseFailure
import dev.vulnlog.lib.result.ParseResult

/**
 * One block per failed file: a header naming the file, then every problem with its source
 * position and entry path when known.
 */
fun renderParseFailure(
    filename: String,
    failure: ParseResult.Error,
): String =
    buildString {
        append("Parsing of $filename failed:")
        failure.failures.forEach { append("\n${renderFailureLine(it)}") }
    }

private fun renderFailureLine(failure: ParseFailure): String {
    val position = failure.location?.let { "Line ${it.line}:${it.column} - " } ?: ""
    val path = failure.path?.let { "$it: " } ?: ""
    return "[ERROR] $position$path${failure.message}"
}
