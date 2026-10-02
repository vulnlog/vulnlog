// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import java.nio.file.Path

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

fun isVulnlogFileName(name: String): Boolean =
    name == "vulnlog.yaml" || name == "vulnlog.yml" || name.endsWith(".vl.yaml") || name.endsWith(".vl.yml")

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
