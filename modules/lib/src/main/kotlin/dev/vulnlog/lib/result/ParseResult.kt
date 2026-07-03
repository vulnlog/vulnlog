// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.shell.FileInputOption

sealed interface ParseResult {
    data class Ok(
        val validationVersion: ParseValidationVersion,
        val content: VulnlogFile,
        val rawContent: VulnlogFileRaw,
    ) : ParseResult

    data class Error(
        val error: String,
        val location: FailureLocation? = null,
    ) : ParseResult
}

data class ParseResults(
    val success: Map<FileInputOption, ParseResult.Ok> = emptyMap(),
    val failure: Map<FileInputOption, ParseResult.Error> = emptyMap(),
)
