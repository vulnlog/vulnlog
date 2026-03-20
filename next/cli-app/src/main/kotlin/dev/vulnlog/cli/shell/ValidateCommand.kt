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
import dev.vulnlog.cli.core.renderValidation
import dev.vulnlog.cli.core.validate
import dev.vulnlog.cli.model.VulnlogFileContext
import dev.vulnlog.cli.parse.YamlParser
import dev.vulnlog.cli.parse.createYamlMapper
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.result.ValidationResult
import java.io.File
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
        val parser = YamlParser(createYamlMapper())

        // parse and check for parsing errors
        val parseResults: Map<File, ParseResult> =
            files
                .map { path -> path.toFile() }
                .associateWith { file -> parser.parse(file.readText()) }
        if (parseResults.values.any { it is ParseResult.Error }) {
            for ((file: File, result: ParseResult) in parseResults) {
                if (result is ParseResult.Error) {
                    echo("Parsing of ${file.name} failed:", err = true)
                    result.errors.forEach { echo(it, err = true) }
                }
            }
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        // validate and check for validation findings
        val validationFindings: Map<VulnlogFileContext, ValidationResult> =
            parseResults
                .map { (file, parseResult) -> file to parseResult as ParseResult.Ok }
                .map { (file, parseResult) ->
                    VulnlogFileContext(
                        parseResult.validationVersion,
                        file.name,
                        parseResult.content,
                    )
                }
                .associateWith { context -> validate(context) }
        if (validationFindings.values.any { it.findings.isNotEmpty() }) {
            val result =
                validationFindings
                    .filter { (_, findings) -> findings.findings.isNotEmpty() }
                    .map { (context, findings) -> context.fileName to renderValidation(findings) }
                    .joinToString("\n\n") { (filename, results) -> "Validation findings for  $filename:\n$results" }
            echo(result, err = true)

            val hasAnyError: Boolean = validationFindings.values.any { it.errors.isNotEmpty() }
            val hasAnyWarning: Boolean = validationFindings.values.any { it.warnings.isNotEmpty() }
            if (hasAnyError || (hasAnyWarning && strict)) {
                throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
            }
        }

        echo("Validation OK")
    }
}
