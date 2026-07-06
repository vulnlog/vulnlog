// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.result.ValidationResult

/**
 * Renders each finding of [result] as one line in the severity grammar, errors first:
 * `error: <file>: <path>: <message>`.
 */
fun renderValidation(
    file: String,
    result: ValidationResult,
): List<String> =
    (result.errors + result.warnings + result.infos).map { finding ->
        formatFinding(finding.severity, file, finding.path, finding.message)
    }
