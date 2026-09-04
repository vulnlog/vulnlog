// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.releasesWithoutPurls
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexProducts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedEntries
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexStatementCounts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexWritten
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.vex.openvex.OpenVexWriter
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DOCUMENT_ID_PREFIX = "https://vulnlog.dev/vex/"

class OpenVexCommand : CliktCommand(name = "openvex") {
    override fun help(context: Context): String = "Generate OpenVEX files from Vulnlog files."

    override fun helpEpilog(context: Context): String =
        """
        |Examples:
        |
        |Write the document to vex.json in the current directory.
        |
        |vulnlog vex openvex vulnlog.yaml
        |
        |Write the document to stdout.
        |
        |vulnlog vex openvex vulnlog.yaml -o -
        """.trimMargin()

    val input: FileInputOption by argument(
        help = "Vulnlog file, or '-' to read from stdin.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to vex.json in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vex.json")))

    override fun run() {
        val vulnlogFile = validateInputOrFail(input).project.vulnlogProjectFile

        warnAboutSkippedReleases(vulnlogFile)
        renderOpenVexProducts(vulnlogFile)?.let { diagnosticSink().verbose(it) }
        val document =
            buildOpenVexDocument(
                vulnlogFile = vulnlogFile,
                id = DOCUMENT_ID_PREFIX + UUID.randomUUID(),
                timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            )
        renderOpenVexSkippedEntries(vulnlogFile).forEach { diagnosticSink().debug(it) }
        if (document.statements.isEmpty()) failOnEmptyDocument(vulnlogFile)
        diagnosticSink().verbose(renderOpenVexStatementCounts(document))

        val content = OpenVexWriter.write(document)
        when (val target = output) {
            is FileOutputOption.File -> {
                writeReport({ echoStatus(it) }, { echoMessage(it) }, target, content)
                diagnosticSink().verbose(renderOpenVexWritten(target.path.toString(), document))
            }
            FileOutputOption.Stdout -> {
                echo(content, trailingNewline = false)
                diagnosticSink().verbose(renderOpenVexWritten("<stdout>", document))
            }
        }
    }

    /** Names the releases a statement was meant for that carry no purl, because they silently drop out. */
    private fun warnAboutSkippedReleases(vulnlogFile: VulnlogFile) {
        val skipped =
            vulnlogFile.vulnerabilities
                .flatMap { vulnEntry -> releasesWithoutPurls(vulnlogFile, vulnEntry) }
                .toSet()
        if (skipped.isEmpty()) return
        // Named in the order the file declares them, not the order the entries happen to mention them.
        val names =
            vulnlogFile.releases
                .map(ReleaseEntry::id)
                .filter { it in skipped }
                .joinToString(", ") { "'${it.value}'" }
        echoMessage(
            formatMessage(FindingSeverity.WARNING, "releases without purls are not part of the document: $names"),
        )
    }

    private fun failOnEmptyDocument(vulnlogFile: VulnlogFile): Nothing {
        echoMessage(formatMessage(FindingSeverity.ERROR, "no statement applies"))
        val hint =
            if (vulnlogFile.releases.none { it.purls.isNotEmpty() }) {
                "declare 'purls' on the releases you want the document to cover"
            } else {
                "no vulnerability entry references a release that declares purls"
            }
        echoMessage(formatHint(hint))
        throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
    }
}
