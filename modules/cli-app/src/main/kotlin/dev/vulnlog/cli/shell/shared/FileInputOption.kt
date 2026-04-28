// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import dev.vulnlog.lib.result.InputValidationResult
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

sealed interface FileInputOption {
    data object Stdin : FileInputOption

    data class File(
        val path: Path,
    ) : FileInputOption
}

fun ArgumentTransformContext.toInputFileOption(input: String): FileInputOption =
    if (input == "-") {
        FileInputOption.Stdin
    } else {
        toInputFile(input)
    }

fun ArgumentTransformContext.toInputFile(input: String): FileInputOption.File {
    val inputPath = Path.of(input)
    if (inputPath.isDirectory() || !inputPath.exists()) {
        fail("Input path '$inputPath' is a directory or file does not exist.")
    }
    val inputFileValidation = validateInputPath(inputPath)
    if (inputFileValidation is InputValidationResult.Error) {
        fail("Input '$inputPath' is not valid: ${inputFileValidation.message}")
    }
    return FileInputOption.File(inputPath)
}
