// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.insertEntryAfterVulnerabilitiesHeader
import dev.vulnlog.lib.core.latestPublishedRelease
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.core.serializeEntryYaml
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import dev.vulnlog.lib.shell.FileInputOption
import kotlin.io.path.readText
import kotlin.io.path.writeText

// TODO move copy after modify command: vulnlog modify copy ...
class CopyCommand : CliktCommand(name = "copy") {
    override fun help(context: Context): String =
        """
        |Copy vulnerability entries from a source file into one or more target files.
        |The copied entry's release is set to the latest published release.
        """.trimMargin()

    val source: FileInputOption.File by argument(help = "Source Vulnlog file to copy vulnerabilities from.")
        .convert(conversion = ArgumentTransformContext::toInputFile)

    val destinations: List<FileInputOption.File> by argument(
        help = "Target Vulnlog file(s) to past vulnerabilities into.",
    ).convert(conversion = ArgumentTransformContext::toInputFile)
        .multiple(required = true)

    val vulnIds: Set<VulnId> by option(
        "--vuln-id",
        help = "Vulnerability ID to copy (repeatable)",
    ).convert { parseVulnId(it) }
        .multiple(required = true)
        .unique()

    override fun run() {
        val parsedSuccessfully = parseInputOrFail(listOf(source))
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val sourceVulnlogFile = parsedSuccessfully.values.first().content
        val vulnIdsNotInSourceContent = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, vulnIds)
        if (vulnIdsNotInSourceContent.isNotEmpty()) {
            echo(
                "Error: Vulnerability IDs not found in source file: ${vulnIdsNotInSourceContent.joinToString(", ")}",
                err = true,
            )
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val mapper = createYamlMapper()

        for (destination in destinations) {
            val parsedTargetSuccessfully = parseInputOrFail(listOf(destination))
            validateParsedInputOrFailWithFailureOutput(parsedTargetSuccessfully)

            val destinationVulnlogFile = parsedTargetSuccessfully.values.first().content
            val vulnIdsNotInTargetContent = findNonExistingVulnIds(destinationVulnlogFile.vulnerabilities, vulnIds)
            val vulnIdsToIgnore = vulnIds.minus(vulnIdsNotInTargetContent)
            if (vulnIdsToIgnore.isNotEmpty()) {
                echo(
                    "Warning: Skipping IDs already exist in ${destination.path}: ${
                        vulnIdsToIgnore.joinToString(
                            ", ",
                        ) { it.id }
                    }",
                    err = true,
                )
            }
            val latestRelease = latestPublishedRelease(destinationVulnlogFile.releases)

            val vulnEntries =
                sourceVulnlogFile.vulnerabilities
                    .filter { it.id in vulnIdsNotInTargetContent }
                    // to maintain order when inserting entries as the first one in insertEntryAfterVulnerabilitiesHeader
                    .reversed()

            var destinationContent = destination.path.readText()
            for (entry in vulnEntries) {
                val dto = V1Mapper.vulnerabilityToDto(entry)
                val adjustedDto = dto.copy(releases = listOf(latestRelease.value))
                val entryYaml = serializeEntryYaml(adjustedDto, mapper)
                destinationContent = insertEntryAfterVulnerabilitiesHeader(destinationContent, entryYaml)
            }
            destination.path.writeText(destinationContent)
            echo("Copied to ${destination.path}: ${vulnIdsNotInTargetContent.joinToString(", ") { it.id }}")
        }
    }
}
