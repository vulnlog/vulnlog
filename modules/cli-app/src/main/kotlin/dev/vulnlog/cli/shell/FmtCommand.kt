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
import dev.vulnlog.lib.core.formatYaml
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.sourceFile
import kotlin.io.path.writeText

class FmtCommand : CliktCommand(name = "fmt") {
    override fun help(context: Context): String =
        """
        |Format Vulnlog file(s) to the canonical style: stable field order, single-element flow
        |arrays, folded block scalars for long text, and minimal type-safe quoting.
        |Files are formatted in place; '-' reads a single document from STDIN and writes the
        |formatted result to STDOUT. STDIN cannot be combined with files.
        """.trimMargin()

    private val inputs: List<FileInputOption> by argument(
        help = "Vulnlog file(s) to format, or '-' to read from stdin.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)
        .multiple(required = true)

    private val isCheck: Boolean by option(
        "--check",
        help = "Do not write changes; exit non-zero if any file is not already formatted.",
    ).flag(default = false)

    override fun run() {
        printOutputSeparator()
        val parsed = parseInputOrFail(inputs)
        validateParsedInputOrFailWithFailureOutput(parsed)

        val mapper = createYamlMapper()
        var anyUnformatted = false
        for (input in inputs) {
            val raw = parsed.getValue(input.sourceFile()).rawContent
            val formatted = formatYaml(raw, mapper)
            val alreadyFormatted = formatted == raw
            if (!alreadyFormatted) anyUnformatted = true

            when (input) {
                FileInputOption.Stdin -> if (!isCheck) echo(formatted, trailingNewline = false)
                is FileInputOption.File ->
                    when {
                        isCheck && !alreadyFormatted -> echo("Can be reformatted: ${input.path}")
                        alreadyFormatted -> echo("Already formatted: ${input.path}")
                        else -> {
                            input.path.writeText(formatted)
                            echo("Formatted: ${input.path}")
                        }
                    }
            }
        }

        if (isCheck && anyUnformatted) {
            throw ProgramResult(ExitCode.FORMAT_ERROR.ordinal)
        }
    }
}
