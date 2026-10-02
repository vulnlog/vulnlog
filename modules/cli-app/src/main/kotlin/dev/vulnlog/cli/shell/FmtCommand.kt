// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.shell.validation.parseInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.document.FormatOutcome
import dev.vulnlog.lib.document.checkFormat
import dev.vulnlog.lib.document.formatCommentsDroppedWarning
import dev.vulnlog.lib.document.formatYamlOutcome
import dev.vulnlog.lib.document.renderFormatFinding
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.yaml.hasYamlComments
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.io.DiagnosticLevel
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.writeOutput

class FmtCommand : CliktCommand(name = "fmt") {
    override fun help(context: Context): String =
        """
        |Format Vulnlog file(s) to the canonical style.
        |The command rewrites the file in-place, when file(s) are specified.
        |When read from STDIN, the command writes the formatted content to STDOUT.
        """.trimMargin()

    private val inputs: List<FileInputOption> by
        vulnlogFileInputs("Vulnlog file(s) to format, or '-' to read from stdin.")

    private val isCheck: Boolean by option(
        "--check",
        help =
            """
            |Check Vulnlog file(s) formatting without modifying them.
            |Exit code ${ExitCode.FORMAT_ERROR.code} if any file is not already formatted.
            """.trimMargin(),
    ).flag(default = false)

    /**
     * Formatting rewrites the document as written, so it stops after the DTO stage: a file whose
     * domain rules do not hold is still formattable, and often needs formatting to be readable.
     */
    override fun run() {
        val parsed: List<ParsedVulnlogProject> =
            inputs.map { input -> parseInputOrFail(input).project }

        var anyUnformatted = false
        for (parsedInput in parsed) {
            val source = parsedInput.inputDocument.source
            when (val outcome = formatYamlOutcome(parsedInput)) {
                is FormatOutcome.Unchanged -> reportUnchanged(parsedInput, source)

                is FormatOutcome.Reformatted -> {
                    anyUnformatted = true
                    if (isCheck) {
                        echoFormatCheckFindings(parsedInput, source)
                    } else {
                        writeReformatted(parsedInput, source, outcome.formatted)
                    }
                }
            }
        }

        if (isCheck && anyUnformatted) {
            throw ProgramResult(ExitCode.FORMAT_ERROR.code)
        }
    }

    private fun reportUnchanged(
        parsedInput: ParsedVulnlogProject,
        source: String,
    ) {
        when (parsedInput.inputDocument.path) {
            null -> if (!isCheck) echo(parsedInput.inputDocument.content, trailingNewline = false)
            else -> echoStatus(formatStatus(StatusVerb.UNCHANGED, source))
        }
    }

    private fun writeReformatted(
        parsedInput: ParsedVulnlogProject,
        source: String,
        formatted: String,
    ) {
        if (hasYamlComments(parsedInput.nodeTree.rootNode)) {
            echoMessage(formatCommentsDroppedWarning(source))
        }
        debugFormatFindings(parsedInput)
        when (val path = parsedInput.inputDocument.path) {
            null -> echo(formatted, trailingNewline = false)
            else -> {
                writeOrFail(writeOutput(path, formatted))
                diagnosticSink().verbose("wrote $source")
                echoStatus(formatStatus(StatusVerb.FORMATTED, source))
            }
        }
    }

    private fun echoFormatCheckFindings(
        parsedInput: ParsedVulnlogProject,
        source: String,
    ) {
        echoMessage(formatFinding(FindingSeverity.WARNING, source, message = "not canonically formatted"))
        checkFormat(parsedInput).forEach { finding ->
            echoMessage("  ${renderFormatFinding(finding)}")
        }
    }

    private fun debugFormatFindings(parsedInput: ParsedVulnlogProject) {
        if (!diagnostics().verbosity.enables(DiagnosticLevel.DEBUG)) return
        checkFormat(parsedInput).forEach { finding ->
            diagnosticSink().debug(renderFormatFinding(finding))
        }
    }
}
