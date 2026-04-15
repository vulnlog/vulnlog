// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.core.insertEntryAfterVulnerabilitiesHeader
import dev.vulnlog.cli.core.latestPublishedRelease
import dev.vulnlog.cli.core.serializeEntryYaml
import dev.vulnlog.cli.parse.createYamlMapper
import dev.vulnlog.cli.parse.v1.V1Mapper
import dev.vulnlog.cli.result.InputValidationResult
import dev.vulnlog.cli.result.ParseResult
import dev.vulnlog.cli.shell.shared.parseFile
import dev.vulnlog.cli.shell.shared.validateFiles
import dev.vulnlog.cli.shell.shared.validateInputPath
import java.nio.file.Path

class CopyCommand : CliktCommand(name = "copy") {
    override val hiddenFromHelp = true

    override fun help(context: Context): String =
        "Copy vulnerability entries from a source file into one or more target files."

    val source: String by argument(help = "Source Vulnlog file")
    val targets: List<String> by argument(help = "Target Vulnlog file(s)").multiple()

    val vulnIds: List<String> by option(
        "--vuln-id",
        help = "Vulnerability ID to copy (repeatable)",
    ).multiple(required = true)

    override fun run() {
        if (targets.isEmpty()) {
            echo("Error: At least one target file is required.", err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val sourceResult = parseAndValidateSingle(Path.of(source))
        val sourceFile = sourceResult.content

        val entries =
            vulnIds.map { vulnId ->
                sourceFile.vulnerabilities.find { it.id.id == vulnId }
                    ?: run {
                        echo("Error: Vulnerability '$vulnId' not found in source file.", err = true)
                        throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
                    }
            }

        val mapper = createYamlMapper()

        for (targetPathStr in targets) {
            val targetPath = Path.of(targetPathStr)
            val targetResult = parseAndValidateSingle(targetPath)
            val targetFile = targetResult.content

            val existingIds = targetFile.vulnerabilities.map { it.id.id }.toSet()

            val latestRelease = latestPublishedRelease(targetFile.releases)
            if (latestRelease == null) {
                echo("Warning: No published release found in ${targetPath.fileName}. Skipping.", err = true)
                continue
            }

            var fileContent = targetPath.toFile().readText()

            for (entry in entries) {
                val entryId = entry.id.id
                if (entryId in existingIds) {
                    echo("Warning: '$entryId' already exists in ${targetPath.fileName}. Skipping.", err = true)
                    continue
                }

                val dto = V1Mapper.vulnerabilityToDto(entry)
                val adjustedDto = dto.copy(releases = listOf(latestRelease.value))
                val entryYaml = serializeEntryYaml(adjustedDto, mapper)
                fileContent = insertEntryAfterVulnerabilitiesHeader(fileContent, entryYaml)

                echo("Copied '$entryId' to ${targetPath.fileName}")
            }

            targetPath.toFile().writeText(fileContent)
        }
    }

    private fun parseAndValidateSingle(path: Path): ParseResult.Ok {
        val inputResult = validateInputPath(path)
        if (inputResult is InputValidationResult.Error) {
            echo(inputResult.message, err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val parseResults = parseFile(path)
        parseResults.onEachFailure { file, result ->
            echo("Parsing of ${file.name} failed:", err = true)
            echo(result.error, err = true)
        }
        if (parseResults.failure.isNotEmpty()) {
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        val validationFindings = validateFiles(parseResults.success)
        if (validationFindings.renderedFindings.isNotBlank() && validationFindings.hasErrors) {
            echo(validationFindings.renderedFindings, err = true)
            throw ProgramResult(ExitCode.VALIDATION_ERROR.ordinal)
        }

        return parseResults.success.values.first()
    }
}
