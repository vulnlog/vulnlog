// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import dev.vulnlog.lib.result.ValidationResults
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.parseInputs
import dev.vulnlog.lib.shell.validateFiles
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import java.io.File

fun parseInputOrFail(inputFiles1: List<FileInputOption.File>): Map<File, ParseResult.Ok> {
    val parseResults: ParseResults =
        try {
            parseInputs(inputFiles1)
        } catch (e: IllegalArgumentException) {
            throw GradleException(e.message ?: "Unknown error during parsing")
        } catch (e: IllegalStateException) {
            throw GradleException(e.message ?: "Unknown error during parsing")
        }
    val messages =
        parseResults.failure.map { (file, result) ->
            "Parsing of ${file.name} failed:\n${(result as ParseResult.Error).error}"
        }
    if (parseResults.failure.isNotEmpty()) {
        throw GradleException(messages.joinToString("\n\n"))
    }
    return parseResults.success
}

fun DefaultTask.validateParsedInputOrFailWithFailureOutput(fileToResult: Map<File, ParseResult.Ok>): ValidationResults {
    val validationFindings = validateFiles(fileToResult)
    if (validationFindings.renderedFindings.isNotBlank()) {
        logger.warn(validationFindings.renderedFindings)
    }
    if (validationFindings.hasErrors) {
        throw GradleException("Vulnlog validation failed.")
    }
    return validationFindings
}
