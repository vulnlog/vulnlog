// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.VulnlogFile
import java.io.File

sealed interface ParseResult {
    data class Ok(
        val validationVersion: ParseValidationVersion,
        val content: VulnlogFile,
    ) : ParseResult

    data class Error(
        val error: String,
    ) : ParseResult
}

data class ParseResults(
    val success: Map<File, ParseResult.Ok> = emptyMap(),
    val failure: Map<File, ParseResult.Error> = emptyMap(),
) {
    fun onEachFailure(action: (File, ParseResult.Error) -> Unit) = failure.forEach(action)
}
