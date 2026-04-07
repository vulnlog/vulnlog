package dev.vulnlog.cli.result

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.VulnlogFile
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
