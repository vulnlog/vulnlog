// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.lib.core.FormatOutcome
import dev.vulnlog.lib.core.checkFormat
import dev.vulnlog.lib.core.formatCommentsDroppedWarning
import dev.vulnlog.lib.core.formatYamlOutcome
import dev.vulnlog.lib.core.renderFormatFinding
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.shell.DiagnosticLevel
import dev.vulnlog.lib.shell.FileInputOption
import tools.jackson.databind.ObjectMapper
import kotlin.io.path.writeText

class FmtCommand : CliktCommand(name = "fmt") {
    override fun help(context: Context): String =
        """
        |Format Vulnlog file(s) to the canonical style.
        |The command rewrites the file in-place, when file(s) are specified.
        |When read from STDIN, the command writes the formatted content to STDOUT.
        """.trimMargin()

    private val inputs: List<FileInputOption> by argument(
        help = "Vulnlog file(s) to format, or '-' to read from stdin.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)

    private val isCheck: Boolean by option(
        "--check",
        help =
            """
            |Check Vulnlog file(s) formatting without modifying them.
            |Exit code ${ExitCode.FORMAT_ERROR.ordinal} if any file is not already formatted.
            """.trimMargin(),
    ).flag(default = false)

    override fun run() {
        val parsed = parseInputOrFail(inputs)

        val mapper = createYamlMapper()
        var anyUnformatted = false
        for (input in inputs) {
            val parsedInput = parsed.getValue(input)
            val raw = parsedInput.rawContent
            val outcome = formatYamlOutcome(parsedInput, mapper)
            if (outcome is FormatOutcome.Reformatted) anyUnformatted = true

            when (input) {
                FileInputOption.Stdin ->
                    when (outcome) {
                        is FormatOutcome.Unchanged -> if (!isCheck) echo(raw.content, trailingNewline = false)
                        is FormatOutcome.Reformatted ->
                            if (isCheck) {
                                echoCheckFindings("<stdin>", parsedInput, mapper)
                            } else {
                                if (hasYamlComments(parsedInput.rootNode)) {
                                    echo(formatCommentsDroppedWarning("<stdin>"), err = true)
                                }
                                debugFormatFindings(parsedInput, mapper)
                                echo(outcome.formatted.content, trailingNewline = false)
                            }
                    }

                is FileInputOption.File ->
                    when (outcome) {
                        is FormatOutcome.Unchanged -> echoStatus("Already formatted: ${input.path}")
                        is FormatOutcome.Reformatted ->
                            if (isCheck) {
                                echoCheckFindings(input.path.toString(), parsedInput, mapper)
                            } else {
                                if (hasYamlComments(parsedInput.rootNode)) {
                                    echo(formatCommentsDroppedWarning(input.path.toString()), err = true)
                                }
                                debugFormatFindings(parsedInput, mapper)
                                input.path.writeText(outcome.formatted.content)
                                diagnosticSink().verbose("wrote ${input.path}")
                                echoStatus("Formatted: ${input.path}")
                            }
                    }
            }
        }

        if (isCheck && anyUnformatted) {
            throw ProgramResult(ExitCode.FORMAT_ERROR.ordinal)
        }
    }

    private fun echoCheckFindings(
        source: String,
        parsedInput: ParseResult.Ok,
        mapper: ObjectMapper,
    ) {
        echoStatus("Can be reformatted: $source")
        checkFormat(parsedInput, mapper).forEach { finding ->
            echoStatus("  ${renderFormatFinding(finding)}")
        }
    }

    private fun debugFormatFindings(
        parsedInput: ParseResult.Ok,
        mapper: ObjectMapper,
    ) {
        if (!diagnostics().verbosity.enables(DiagnosticLevel.DEBUG)) return
        checkFormat(parsedInput, mapper).forEach { finding ->
            diagnosticSink().debug(renderFormatFinding(finding))
        }
    }
}
