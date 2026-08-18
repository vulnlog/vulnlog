// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.validation

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.lib.core.validation.ValidationOutcome
import dev.vulnlog.lib.core.validation.parseDocument
import dev.vulnlog.lib.core.validation.renderFindings
import dev.vulnlog.lib.core.validation.renderParsedProject
import dev.vulnlog.lib.core.validation.renderProblem
import dev.vulnlog.lib.core.validation.validateDocument
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.InputDocument
import dev.vulnlog.lib.shell.ValidationRequest
import dev.vulnlog.lib.shell.readInputDocument
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

private const val VALIDATION_FAILED = "Vulnlog validation failed."

/** Reads [input] to DTO and validates on DTO-level. Any finding is reported to the corresponding Gradle log level. Fails the Gradle build on any finding. */
fun DefaultTask.parseInputOrFail(
    input: FileInputOption,
    validationRequest: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ParsedVulnlogProject> {
    val document = readInputDocument(input)
    return unwrap(parseDocument(document, validationRequest.config), document, validationRequest.reportedSeverities)
}

/** Reads [input] to Domain and validates on domain-level. */
fun DefaultTask.validateInputOrFail(
    input: FileInputOption,
    request: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ValidVulnlogProject> {
    val document = readInputDocument(input)
    val ok = unwrap(validateDocument(document, request.config), document, request.reportedSeverities)
    diagnosticSink().verbose(renderParsedProject(document.filename, ok.project.vulnlogProjectFile))
    return ok
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

        is ValidationOutcome.Stopped.Unreadable -> {
            outcome.problems.forEach { problem -> logMessage(renderProblem(document.filename, problem)) }
            throw GradleException(VALIDATION_FAILED)
        }

        is ValidationOutcome.Stopped.Rejected -> throw GradleException(VALIDATION_FAILED)
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
