// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

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
        return InputValidationResult.Error("Path '$path' does not exist.")
    }
    val name = path.fileName.toString()
    if (!isVulnlogFileName(name)) {
        return InputValidationResult.Error("File name must be [vulnlog|*.vl].[yaml|yml]: $path")
    }
    return InputValidationResult.Ok(path)
}

/** Whether [name] is a Vulnlog file name: `vulnlog.yaml`, `vulnlog.yml`, or `*.vl.yaml` / `*.vl.yml`. */
fun isVulnlogFileName(name: String): Boolean =
    name == "vulnlog.yaml" || name == "vulnlog.yml" || name.endsWith(".vl.yaml") || name.endsWith(".vl.yml")

/** Validates how `<stdin>` combines with file inputs across a multi-input command: at most one `<stdin>`, and never mixed with files. */
fun validateInputSelection(inputs: List<FileInputOption>): InputSelectionResult {
    val stdinCount = inputs.count { it is FileInputOption.Stdin }
    val hasFiles = inputs.any { it is FileInputOption.File }
    if (stdinCount > 1) {
        return InputSelectionResult.Error("Multiple <stdin> are not supported.")
    }
    if (stdinCount == 1 && hasFiles) {
        return InputSelectionResult.Error("Mixing input files with STDIN is not allowed.")
    }
    return InputSelectionResult.Ok
}
