// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell.shared

import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

sealed interface FileOutputOption {
    data object Stdout : FileOutputOption

    data class File(
        val path: Path,
    ) : FileOutputOption
}

sealed interface DirectoryOutputOption {
    data object Stdout : DirectoryOutputOption

    data class Directory(
        val path: Path,
    ) : DirectoryOutputOption
}

fun OptionCallTransformContext.toOutputFileOption(output: String): FileOutputOption =
    if (output == "-") {
        FileOutputOption.Stdout
    } else {
        val outputPath = Path.of(output)
        if (outputPath.isDirectory()) {
            fail("Output path '$outputPath' is a directory, expected a file.")
        }
        FileOutputOption.File(outputPath)
    }

fun OptionCallTransformContext.toOutputDirectoryOption(output: String): DirectoryOutputOption =
    if (output == "-") {
        DirectoryOutputOption.Stdout
    } else {
        val outputPath = Path.of(output)
        if (!outputPath.isDirectory()) {
            fail("Output path '$outputPath' is not a directory.")
        }
        DirectoryOutputOption.Directory(outputPath)
    }

fun writeInit(
    out: (String) -> Unit,
    err: (String) -> Unit,
    initFile: FileOutputOption.File,
    content: String,
) {
    try {
        initFile.path.writeText(content)
        out("Vulnlog file created at: ${initFile.path.toAbsolutePath()}")
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}

fun writeSuppress(
    out: (String) -> Unit,
    err: (String) -> Unit,
    outputDir: DirectoryOutputOption.Directory,
    content: SuppressionFile,
) {
    val outputPath = outputDir.path.resolve(content.fileName)
    try {
        outputPath.writeText(content.content)
        out("Suppression file created at: ${outputPath.toAbsolutePath()}")
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}

fun writeReport(
    out: (String) -> Unit,
    err: (String) -> Unit,
    reportFile: FileOutputOption.File,
    content: String,
) {
    try {
        reportFile.path.writeText(content)
        out("Report written to: ${reportFile.path.toAbsolutePath()}")
    } catch (e: Exception) {
        err("Error writing file: ${e.message}")
        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
    }
}
