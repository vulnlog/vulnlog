// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import dev.vulnlog.lib.result.InputValidationResult
import java.nio.file.Path

/**
 * Validates the provided file path based on its existence and naming conventions.
 *
 * @param path the file path to be validated.
 * @return an instance of [InputValidationResult], which is either:
 * - `Ok` if the path exists and the file name follows the required pattern.
 * - `Error` if the path does not exist or the file name is invalid.
 */
fun validateInputPath(path: Path): InputValidationResult {
    if (!path.toFile().exists()) {
        return InputValidationResult.Error("Error: Path '$path' does not exist.")
    }
    val name = path.fileName.toString()
    if (!isVulnlogFileName(name)) {
        return InputValidationResult.Error("Error: File name must be [vulnlog|*.vl].[yaml|yml]: $path")
    }
    return InputValidationResult.Ok(path)
}

private fun isVulnlogFileName(name: String): Boolean =
    name == "vulnlog.yaml" || name.endsWith(".vl.yaml") || name.endsWith(".vl.yml")
