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
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.copyVulnerabilities
import dev.vulnlog.lib.core.findNonExistingVulnIds
import dev.vulnlog.lib.core.formatCommentsDroppedWarning
import dev.vulnlog.lib.core.formatCopiedMessage
import dev.vulnlog.lib.core.formatVulnIdsNotInSourceMessage
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
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
        val validatedSource = validateInputOrFail(source).project
        val sourceVulnlogFile = validatedSource.vulnlogProjectFile
        val missing = findNonExistingVulnIds(sourceVulnlogFile.vulnerabilities, vulnIds)
        if (missing.isNotEmpty()) {
            echoMessage(formatVulnIdsNotInSourceMessage(missing))
            throw ProgramResult(ExitCode.GENERAL_ERROR.code)
        }

        val validatedDestinations: List<ValidVulnlogProject> =
            destinations.map { input -> validateInputOrFail(input).project }
        validatedDestinations.forEach { validDestination ->
            val outcome =
                copyVulnerabilities(
                    source = sourceVulnlogFile,
                    destination = validDestination,
                    vulnIds = vulnIds,
                )
            val source = validDestination.inputDocument.source
            if (hasYamlComments(validDestination.nodeTree.rootNode)) {
                echoMessage(formatCommentsDroppedWarning(source))
            }
            val destinationPath =
                requireNotNull(validDestination.inputDocument.path) { "copy destinations are always files" }
            destinationPath.writeText(outcome.newContent)
            diagnosticSink().verbose("wrote $source")
            if (outcome.copied.isNotEmpty()) {
                diagnosticSink().verbose("copied to $source: ${outcome.copied.joinToString(", ") { it.id }}")
            }
            echoStatus(formatCopiedMessage(destinationPath, outcome.copied))
        }
    }
}
