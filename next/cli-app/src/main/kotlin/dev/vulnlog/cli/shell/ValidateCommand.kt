package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.result.InputValidationResult
import dev.vulnlog.cli.shell.shared.merge
import dev.vulnlog.cli.shell.shared.parseFiles
import dev.vulnlog.cli.shell.shared.parseStdin
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.cli.shell.shared.validateInputPath
import java.nio.file.Path

class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate YAML files for Vulnlog configuration."

    val args: List<String> by argument().multiple()

    val strict: Boolean by option("--strict", help = "Treats warnings as errors.").flag(default = false)

    override fun run() {
        val hasStdin = args.contains("-")
        val filePaths = args.filter { it != "-" }.map { Path.of(it) }

        filePaths.forEach { file ->
            val result = validateInputPath(file)
            if (result is InputValidationResult.Error) {
                echo(result.message, err = true)
                throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
            }
        }

        if (!hasStdin && filePaths.isEmpty()) {
            echo("Error: No input provided. Pass file paths or '-' for stdin.", err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val stdinResults = if (hasStdin) parseStdin() else null
        val fileResults = if (filePaths.isNotEmpty()) parseFiles(filePaths) else null

        val parseResults = merge(stdinResults, fileResults)

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
