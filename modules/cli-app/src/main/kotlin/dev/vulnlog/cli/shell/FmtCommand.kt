// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.shell.validation.parseInputOrFail
import dev.vulnlog.lib.app.FormatOutcome
import dev.vulnlog.lib.app.FormatRequest
import dev.vulnlog.lib.app.formatDocument
import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderFormatMessages
import dev.vulnlog.lib.render.renderWritten

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

    override fun run() {
        val projects = inputs.map { input -> parseInputOrFail(input).project }
        val request = FormatRequest(check = isCheck)

        val outcomes = projects.map { project -> formatDocument(project, request) }
        outcomes.forEach { outcome ->
            renderFormatMessages(outcome).forEach(::echoMessage)
            when (outcome) {
                is FormatOutcome.Unchanged -> reportUnchanged(outcome.document)
                is FormatOutcome.Reformatted -> write(outcome.document, outcome.formatted)
                is FormatOutcome.NotCanonical -> Unit
            }
        }

        if (outcomes.any { it is FormatOutcome.NotCanonical }) {
            throw ProgramResult(ExitCode.FORMAT_ERROR.code)
        }
    }

    /** Standard input has no file to leave alone, so its content goes to standard output as the result. */
    private fun reportUnchanged(document: InputDocument) {
        when (document.path) {
            null -> if (!isCheck) echo(document.content, trailingNewline = false)
            else -> echoMessage(Message.Status(formatStatus(StatusVerb.UNCHANGED, document.source)))
        }
    }

    private fun write(
        document: InputDocument,
        formatted: String,
    ) {
        when (val path = document.path) {
            null -> echo(formatted, trailingNewline = false)

            else -> {
                writeOrFail(writeOutput(path, formatted))
                echoMessage(renderWritten(document.source))
                echoMessage(Message.Status(formatStatus(StatusVerb.FORMATTED, document.source)))
            }
        }
    }
}
