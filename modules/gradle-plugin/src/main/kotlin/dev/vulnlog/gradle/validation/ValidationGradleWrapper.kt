// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.validation

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.lib.app.InputRejected
import dev.vulnlog.lib.app.ValidationOutcome
import dev.vulnlog.lib.app.ValidationRequest
import dev.vulnlog.lib.app.parseDocument
import dev.vulnlog.lib.app.validateDocument
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.readInputDocument
import dev.vulnlog.lib.render.renderFindings
import dev.vulnlog.lib.render.renderParsedProject
import dev.vulnlog.lib.render.renderProblem
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

private const val VALIDATION_FAILED = "Vulnlog validation failed."

/** Stops at the DTO, so a file whose domain rules fail still loads: for the tasks that only touch the layout. */
fun DefaultTask.parseInputOrFail(
    input: FileInputOption,
    validationRequest: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ParsedVulnlogProject> {
    val document = readOrFail(input)
    return unwrap(parseDocument(document, validationRequest.config), document, validationRequest.reportedSeverities)
}

fun DefaultTask.validateInputOrFail(
    input: FileInputOption,
    request: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ValidVulnlogProject> {
    val document = readOrFail(input)
    val ok = unwrap(validateDocument(document, request.config), document, request.reportedSeverities)
    logger.log(renderParsedProject(document.filename, ok.project.vulnlogProjectFile))
    return ok
}

private fun readOrFail(input: FileInputOption): InputDocument =
    when (val read = readInputDocument(input)) {
        is InputRead.Read -> read.document
        is InputRead.Failed -> throw failure(read)
    }

private fun <T> DefaultTask.unwrap(
    outcome: ValidationOutcome<T>,
    document: InputDocument,
    reportedSeverities: Set<FindingSeverity>,
): ValidationOutcome.Ok<T> {
    val rendered = renderFindings(document.filename, outcome.findings, reportedSeverities)
    if (rendered.isNotBlank()) logMessage(rendered)

    return when (outcome) {
        is ValidationOutcome.Ok -> outcome

        is InputRejected.Unparsable -> {
            outcome.problems.forEach { problem -> logMessage(renderProblem(document.filename, problem)) }
            throw GradleException(VALIDATION_FAILED)
        }

        is InputRejected.Invalid -> throw GradleException(VALIDATION_FAILED)
    }
}

private fun DefaultTask.logMessage(message: String) =
    message.lines().forEach { line ->
        when {
            line.startsWith("error: ") -> logger.error(line)
            line.startsWith("warning: ") -> logger.warn(line)
            else -> logger.lifecycle(line)
        }
    }
