// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.CopiedFile
import dev.vulnlog.lib.app.CopyOutcome
import dev.vulnlog.lib.app.CopyRequest
import dev.vulnlog.lib.app.copyVulnerabilities
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.render.renderCopied
import dev.vulnlog.lib.render.renderCopyFailure
import dev.vulnlog.lib.render.renderCopyMessages
import dev.vulnlog.lib.render.renderWritten

class CopyCommand : CliktCommand(name = "copy") {
    override fun help(context: Context): String =
        """
        |Copy vulnerability entries from a source file into one or more target files.
        |The copied entry's release is set to the last release the target lists.
        """.trimMargin()

    val source: FileInputOption.File by argument(help = "Source Vulnlog file to copy vulnerabilities from.")
        .convert(conversion = ArgumentTransformContext::toInputFile)

    val destinations: List<FileInputOption.File> by argument(
        help = "Target Vulnlog file(s) to paste vulnerabilities into.",
    ).convert(conversion = ArgumentTransformContext::toInputFile)
        .multiple(required = true)

    val vulnIds: Set<VulnId> by option(
        "--vuln-id",
        help = "Vulnerability ID to copy (repeatable)",
    ).convert { parseVulnId(it) }
        .multiple(required = true)
        .unique()

    override fun run() {
        val sourceProject = validateInputOrFail(source).project
        val destinationProjects = destinations.map { input -> validateInputOrFail(input).project }

        when (val outcome = copyVulnerabilities(sourceProject, destinationProjects, CopyRequest(vulnIds))) {
            is CopyOutcome.Failed -> failWith(renderCopyFailure(outcome), exitCode(outcome))
            is CopyOutcome.Copied -> outcome.files.forEach(::write)
        }
    }

    private fun write(file: CopiedFile) {
        renderCopyMessages(file).forEach(::echoMessage)
        val path = requireNotNull(file.document.path) { "copy destinations are always files" }
        writeOrFail(writeOutput(path, file.content))
        echoMessage(renderWritten(file.document.source))
        renderCopied(file).forEach(::echoMessage)
    }
}
