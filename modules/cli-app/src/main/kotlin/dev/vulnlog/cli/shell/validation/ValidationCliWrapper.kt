// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.validation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.diagnosticSink
import dev.vulnlog.cli.shell.echoHelpHint
import dev.vulnlog.cli.shell.echoMessage
import dev.vulnlog.cli.shell.exitCode
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
import dev.vulnlog.lib.render.formatFailureLines
import dev.vulnlog.lib.render.renderFindings
import dev.vulnlog.lib.render.renderInputFailure
import dev.vulnlog.lib.render.renderParsedProject
import dev.vulnlog.lib.render.renderProblem

/** Reads [input] to DTO and validates on DTO-level. Any finding is reported to stderr. Fails with [ExitCode.VALIDATION_ERROR] on any finding. */
fun CliktCommand.parseInputOrFail(
    input: FileInputOption,
    request: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ParsedVulnlogProject> {
    val document = readOrFail(input)
    return unwrap(parseDocument(document, request.config), document, request.reportedSeverities)
}

/** Reads [input] to Domain and validates on domain-level. */
fun CliktCommand.validateInputOrFail(
    input: FileInputOption,
    validationRequest: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ValidVulnlogProject> {
    val document = readOrFail(input)
    val ok =
        unwrap(validateDocument(document, validationRequest.config), document, validationRequest.reportedSeverities)
    diagnosticSink().verbose(renderParsedProject(document.filename, ok.project.vulnlogProjectFile))
    return ok
}

private fun CliktCommand.readOrFail(input: FileInputOption): InputDocument =
    when (val read = readInputDocument(input)) {
        is InputRead.Read -> read.document

        is InputRead.Failed -> {
            formatFailureLines(listOf(renderInputFailure(read))).forEach(::echoMessage)
            throw ProgramResult(exitCode(read).code)
        }
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

        is InputRejected.Unparsable -> {
            outcome.problems.forEach { problem -> echoMessage(renderProblem(document.filename, problem)) }
            failValidation()
        }

        is InputRejected.Invalid -> failValidation()
    }
}

private fun CliktCommand.failValidation(): Nothing {
    echoHelpHint()
    throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
}
