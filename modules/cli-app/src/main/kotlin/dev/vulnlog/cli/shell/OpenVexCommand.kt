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
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.path
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.cli.shell.vex.resolveOpenVexScope
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.vex.openvex.generateOpenVex
import dev.vulnlog.lib.core.vex.openvex.openVexTooling
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexEmptyHint
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexProducts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedEntries
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedReleases
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexStatementCounts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexWritten
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

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
        |
        |Write the document for the container image of release 1.2.0.
        |
        |vulnlog vex openvex vulnlog.yaml --release 1.2.0 --tag container -o vex-1.2.0-container.json
        |
        |Continue an existing document, so its identifier stays stable and the version counts up.
        |
        |vulnlog vex openvex vulnlog.yaml --baseline vex.json -o vex.json
        """.trimMargin()

    val input: FileInputOption by argument(
        help = "Vulnlog file, or '-' to read from stdin.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)

    val releaseRequest: String? by option(
        "--release",
        metavar = "<release-id>",
        help =
            """
            Write the document for this release only.
            Without it the document covers every release that declares purls.
            """.trimIndent(),
    )

    val tagsRequest: Set<String> by option(
        "--tag",
        metavar = "<tag>",
        help =
            """
            Keep only the release purls carrying this tag.
            Use multiple times to keep the purls carrying any of them.
            """.trimIndent(),
    ).multiple()
        .unique()

    val baselineRequest: Path? by option(
        "--baseline",
        metavar = "<path>",
        help =
            """
            Existing OpenVEX document whose identity is continued.
            Its '@id' and 'timestamp' are kept and 'version' counts up. Without it every run issues a new document.
            """.trimIndent(),
    ).path(canBeDir = false)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to vex.json in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vex.json")))

    override fun run() {
        val vulnlogFile = validateInputOrFail(input).project.vulnlogProjectFile
        val scope = resolveOpenVexScope(releaseRequest, tagsRequest, vulnlogFile)
        val baseline = baselineRequest?.let(::readBaselineOrFail)

        val outcome =
            generateOpenVex(vulnlogFile, scope, baseline, Instant.now(), openVexTooling("CLI", BuildInfo.VERSION))
        echoCollection(outcome.collection)
        val generated =
            when (outcome) {
                is OpenVexOutcome.Empty -> failOnEmptyDocument(vulnlogFile, scope)
                is OpenVexOutcome.Generated -> outcome
            }
        diagnosticSink().verbose(renderOpenVexStatementCounts(generated.document))
        write(generated)
    }

    /** The warning and the diagnostics that say what the collection holds and what it left out. */
    private fun echoCollection(collection: OpenVexCollection) {
        renderOpenVexSkippedReleases(collection)?.let { echoMessage(formatMessage(FindingSeverity.WARNING, it)) }
        renderOpenVexProducts(collection)?.let { diagnosticSink().verbose(it) }
        renderOpenVexSkippedEntries(collection).forEach { diagnosticSink().debug(it) }
    }

    private fun write(generated: OpenVexOutcome.Generated) {
        when (val target = output) {
            is FileOutputOption.File -> {
                if (generated.unchanged && isBaselinePath(target.path)) {
                    echoStatus(formatStatus(StatusVerb.UNCHANGED, target.path.toString()))
                    return
                }
                writeReport({ echoStatus(it) }, { echoMessage(it) }, target, generated.content)
                diagnosticSink().verbose(renderOpenVexWritten(target.path.toString(), generated.document))
            }

            FileOutputOption.Stdout -> {
                echo(generated.content, trailingNewline = false)
                diagnosticSink().verbose(renderOpenVexWritten("<stdout>", generated.document))
            }
        }
    }

    /** True when [target] is the file the baseline was read from, so writing it back would only bump the version. */
    private fun isBaselinePath(target: Path): Boolean =
        baselineRequest?.toAbsolutePath()?.normalize() == target.toAbsolutePath().normalize()

    /**
     * Reads the baseline at [path]. A missing file is an error, because the caller asked to continue a document that
     * is not there. A file that is not an OpenVEX document only warns: garbage in, new identity out.
     */
    private fun readBaselineOrFail(path: Path): OpenVexBaseline? {
        if (!path.isRegularFile()) {
            echoMessage(formatMessage(FindingSeverity.ERROR, "baseline '$path' does not exist"))
            echoMessage(formatHint("omit --baseline to issue a new document"))
            throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
        }
        val content =
            try {
                path.readText()
            } catch (e: IOException) {
                echoMessage(formatMessage(FindingSeverity.ERROR, "cannot read baseline '$path': ${e.message}"))
                throw ProgramResult(ExitCode.GENERAL_ERROR.code)
            }
        return OpenVexReader.readBaseline(content) ?: run {
            echoMessage(
                formatMessage(
                    FindingSeverity.WARNING,
                    "baseline '$path' is not an OpenVEX document, issuing a new one",
                ),
            )
            null
        }
    }

    private fun failOnEmptyDocument(
        vulnlogFile: VulnlogFile,
        scope: OpenVexScope,
    ): Nothing {
        echoMessage(formatMessage(FindingSeverity.ERROR, "no statement applies"))
        echoMessage(formatHint(renderOpenVexEmptyHint(vulnlogFile, scope)))
        throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
    }
}
