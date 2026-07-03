// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.result.ParseResult

/** One block per failed file: a header naming the file, then the error with its source position when known. */
fun renderParseFailure(
    filename: String,
    failure: ParseResult.Error,
): String {
    val position = failure.location?.let { "Line ${it.line}:${it.column} - " } ?: ""
    return "Parsing of $filename failed:\n[ERROR] $position${failure.error}"
}
