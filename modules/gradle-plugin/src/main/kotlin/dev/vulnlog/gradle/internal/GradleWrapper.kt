// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.ParseResults
import dev.vulnlog.lib.result.ValidationResults
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.buildFilter
import dev.vulnlog.lib.shell.parseInputs
import dev.vulnlog.lib.shell.renderParseFailures
import dev.vulnlog.lib.shell.validateFiles
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import java.io.File

fun parseInputOrFail(inputFiles: List<FileInputOption.File>): Map<File, ParseResult.Ok> {
    val parseResults: ParseResults =
        try {
            parseInputs(inputFiles)
        } catch (e: RuntimeException) {
            if (e !is IllegalArgumentException && e !is IllegalStateException) throw e
            throw GradleException(e.message ?: "Unknown error during parsing")
        }
    if (parseResults.failure.isNotEmpty()) {
        throw GradleException(renderParseFailures(parseResults).joinToString("\n\n"))
    }
    return parseResults.success
}

fun buildFilterOrFail(
    vulnlogFile: VulnlogFile,
    reporterOption: String?,
    releaseOption: String?,
    tagsOptions: Set<String>,
): VulnlogFilter =
    try {
        buildFilter(vulnlogFile, reporterOption, releaseOption, tagsOptions)
    } catch (e: FilterValidationException) {
        throw GradleException("${e.message}. ${e.detail}")
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
