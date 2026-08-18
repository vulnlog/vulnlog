// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

sealed interface InputSelectionResult {
    data object Ok : InputSelectionResult

    data class Error(
        val message: String,
    ) : InputSelectionResult
}
