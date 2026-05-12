// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import java.nio.file.Path

sealed interface InputValidationResult {
    data class Ok(
        val path: Path,
    ) : InputValidationResult

    data class Error(
        val message: String,
    ) : InputValidationResult
}
