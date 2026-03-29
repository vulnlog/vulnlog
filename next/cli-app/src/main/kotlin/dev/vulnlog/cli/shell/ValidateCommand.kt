package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import dev.vulnlog.cli.shell.shared.parseFiles
import dev.vulnlog.cli.shell.shared.validateFiles
import java.nio.file.Path

class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate YAML files for Vulnlog configuration."

    val files: List<Path> by argument()
        .path(mustExist = true)
        .multiple()
        .check("file name must be [vulnlog|*.vl].[yaml|yml}") {
            it.all { file ->
                file.fileName.toString() == "vulnlog.yaml" ||
                    file.fileName.toString().endsWith(".vl.yaml") ||
                    file.fileName.toString().endsWith(".vl.yml")
            }
        }

    val strict: Boolean by option("--strict", help = "Treats warnings as errors.").flag(default = false)

    override fun run() {
        val parseResults = parseFiles(files)
        parseResults.onEachFailure { file, result ->
            echo("Parsing of ${file.name} failed:", err = true)
            echo(result.error, err = true)
        }
        if (parseResults.failure.isNotEmpty()) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        val validationFindings = validateFiles(parseResults.success)
        if (validationFindings.renderedFindings.isNotBlank()) {
            echo(validationFindings.renderedFindings, err = true)
        }
        if (validationFindings.hasErrors || (validationFindings.hasWarnings && strict)) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        } else {
            echo("Validation OK")
        }
    }
}
