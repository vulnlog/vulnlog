// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.validation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.diagnosticSink
import dev.vulnlog.cli.shell.echoHelpHint
import dev.vulnlog.cli.shell.echoMessage
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

/** Reads [input] to DTO and validates on DTO-level. Any finding is reported to stderr. Fails with [ExitCode.VALIDATION_ERROR] on any finding. */
fun CliktCommand.parseInputOrFail(
    input: FileInputOption,
    request: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ParsedVulnlogProject> {
    val document = readInputDocument(input)
    return unwrap(parseDocument(document, request.config), document, request.reportedSeverities)
}

/** Reads [input] to Domain and validates on domain-level. */
fun CliktCommand.validateInputOrFail(
    input: FileInputOption,
    validationRequest: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ValidVulnlogProject> {
    val document = readInputDocument(input)
    val ok =
        unwrap(validateDocument(document, validationRequest.config), document, validationRequest.reportedSeverities)
    diagnosticSink().verbose(renderParsedProject(document.filename, ok.project.vulnlogProjectFile))
    return ok
}

private fun <T> CliktCommand.unwrap(
    outcome: ValidationOutcome<T>,
    document: InputDocument,
    reportedSeverities: Set<FindingSeverity>,
): ValidationOutcome.Ok<T> {
    val rendered = renderFindings(document.filename, outcome.findings, reportedSeverities)
    if (rendered.isNotBlank()) echoMessage(rendered)

    return when (outcome) {
        is ValidationOutcome.Ok -> outcome

        is ValidationOutcome.Stopped.Unreadable -> {
            outcome.problems.forEach { problem -> echoMessage(renderProblem(document.filename, problem)) }
            failValidation()
        }

        is ValidationOutcome.Stopped.Rejected -> failValidation()
    }
}

private fun CliktCommand.failValidation(): Nothing {
    echoHelpHint()
    throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
}
