package dev.vulnlog.cli.result

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.VulnlogFile

sealed interface ParseResult {
    data class Ok(val validationVersion: ParseValidationVersion, val content: VulnlogFile) : ParseResult

    data class Error(val errors: List<String>) : ParseResult
}
