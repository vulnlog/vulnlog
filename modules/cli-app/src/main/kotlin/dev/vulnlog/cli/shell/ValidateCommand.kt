// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.core.validation.ValidationOutcome
import dev.vulnlog.lib.core.validation.renderValidationSummary
import dev.vulnlog.lib.model.finding.ALL_SEVERITIES
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.ValidationRequest

class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate Vulnlog YAML files and report issues."

    val inputs: List<FileInputOption> by vulnlogFileInputs("Vulnlog file(s) to validate.")

    val strict: Boolean by option("--strict", help = "Treats warnings as errors.").flag(default = false)

    /** The findings are the output here, so every severity is reported. */
    override fun run() {
        val validationRequest = ValidationRequest(ValidationConfig(strict), ALL_SEVERITIES)
        inputs
            .map { input -> validateInputOrFail(input, validationRequest) }
            .forEach(::printSummary)
    }

    private fun printSummary(validated: ValidationOutcome.Ok<ValidVulnlogProject>) {
        val filename = validated.project.inputDocument.filename
        diagnosticSink().verbose(renderValidationSummary(filename, validated.findings))
        echoStatus(formatStatus(StatusVerb.VALIDATED, filename))
    }
}
