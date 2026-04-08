package dev.vulnlog.cli.result

import java.nio.file.Path

sealed interface InputValidationResult {
    data class Ok(
        val path: Path,
    ) : InputValidationResult

    data class Error(
        val message: String,
    ) : InputValidationResult
}
