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
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.lib.core.copyVulnerabilities
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.formatCopiedMessage
import dev.vulnlog.lib.core.formatVulnIdsNotInSourceMessage
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.shell.FileInputOption
import kotlin.io.path.readText
import kotlin.io.path.writeText

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
        val parsedSource = parseInputOrFail(listOf(source))
        validateParsedInputOrFailWithFailureOutput(parsedSource)
        val sourceVulnlogFile = parsedSource.values.first().content

        val missing = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, vulnIds)
        if (missing.isNotEmpty()) {
            echo(formatVulnIdsNotInSourceMessage(missing), err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        val mapper = createYamlMapper()
        for (destination in destinations) {
            val parsedDestination = parseInputOrFail(listOf(destination))
            validateParsedInputOrFailWithFailureOutput(parsedDestination)
            val destinationVulnlogFile = parsedDestination.values.first().content

            val outcome =
                copyVulnerabilities(
                    source = sourceVulnlogFile,
                    destination = destinationVulnlogFile,
                    destinationContent = destination.path.readText(),
                    vulnIds = vulnIds,
                    mapper = mapper,
                )
            destination.path.writeText(outcome.newContent)
            echo(formatCopiedMessage(destination.path, outcome.copied))
        }
    }
}
