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
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.OpenVexRequest
import dev.vulnlog.lib.app.generateOpenVex
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.vex.openvex.openVexDocumentId
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.render.renderOpenVexBaselineProblem
import dev.vulnlog.lib.render.renderOpenVexEmptyHint
import dev.vulnlog.lib.render.renderOpenVexProducts
import dev.vulnlog.lib.render.renderOpenVexSkippedEntries
import dev.vulnlog.lib.render.renderOpenVexSkippedReleases
import dev.vulnlog.lib.render.renderOpenVexStatementCounts
import dev.vulnlog.lib.render.renderOpenVexWritten
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class OpenVexCommand : CliktCommand(name = "openvex") {
    override fun help(context: Context): String = "Generate OpenVEX files from Vulnlog files. (Incubating feature)"

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
            Existing previous OpenVEX document whose identity is continued.
            Its '@id' is kept but 'timestamp' and 'version' are updated, when the file changes. Without a baseline every run issues a new document.
            """.trimIndent(),
    ).path(canBeDir = false)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to vex.json in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vex.json")))

    /** The OpenVEX version this command reads and writes. The single place an option would feed one day. */
    private val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST

    override fun run() {
        val vulnlogFile = validateInputOrFail(input).project.vulnlogProjectFile
        val scope = resolveOpenVexScope(releaseRequest, tagsRequest, vulnlogFile)
        val request =
            OpenVexRequest(
                scope = scope,
                baseline = baselineRequest?.let(::readBaselineOrFail),
                documentId = openVexDocumentId(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = OpenVexTooling("CLI", BuildInfo.VERSION),
                formatVersion = formatVersion,
            )

        when (val outcome = generateOpenVex(vulnlogFile, request)) {
            is OpenVexOutcome.BaselineRejected ->
                failOnBaseline(renderOpenVexBaselineProblem(baselineRequest.toString(), outcome.problem))

            is OpenVexOutcome.NoStatementApplies -> {
                echoCollection(outcome.collection)
                failOnEmptyDocument(vulnlogFile, scope)
            }

            is OpenVexOutcome.Revised -> write(outcome.collection, outcome.document, outcome.content, unchanged = false)
            is OpenVexOutcome.Unchanged ->
                write(
                    outcome.collection,
                    outcome.document,
                    outcome.content,
                    unchanged = true,
                )
        }
    }

    /** The warning and the diagnostics that say what the collection holds and what it left out. */
    private fun echoCollection(collection: OpenVexCollection) {
        renderOpenVexSkippedReleases(collection)?.let { echoMessage(formatMessage(FindingSeverity.WARNING, it)) }
        renderOpenVexProducts(collection)?.let { diagnosticSink().verbose(it) }
        renderOpenVexSkippedEntries(collection).forEach { diagnosticSink().debug(it) }
    }

    /** Writes [content] to the output. Bytes that stand unchanged are not written back over the baseline. */
    private fun write(
        collection: OpenVexCollection,
        document: OpenVexDocument,
        content: String,
        unchanged: Boolean,
    ) {
        echoCollection(collection)
        diagnosticSink().verbose(renderOpenVexStatementCounts(document))
        when (val target = output) {
            is FileOutputOption.File -> {
                if (unchanged && isBaselinePath(target.path)) {
                    echoStatus(formatStatus(StatusVerb.UNCHANGED, target.path.toString()))
                    return
                }
                writeReport({ echoStatus(it) }, { echoMessage(it) }, target, content)
                diagnosticSink().verbose(renderOpenVexWritten(target.path.toString(), document))
            }

            FileOutputOption.Stdout -> {
                echo(content, trailingNewline = false)
                diagnosticSink().verbose(renderOpenVexWritten("<stdout>", document))
            }
        }
    }

    /** True when [target] is the file the baseline was read from, so writing it back would only bump the version. */
    private fun isBaselinePath(target: Path): Boolean =
        baselineRequest?.toAbsolutePath()?.normalize() == target.toAbsolutePath().normalize()

    /**
     * Reads the text of the baseline at [path]. A missing file is an error, because the caller asked to continue a
     * document that is not there. Whether the text can be continued is the run's to decide.
     */
    private fun readBaselineOrFail(path: Path): String {
        if (!path.isRegularFile()) {
            echoMessage(formatMessage(FindingSeverity.ERROR, "baseline '$path' does not exist"))
            echoMessage(formatHint("omit --baseline to issue a new document"))
            throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
        }
        return try {
            path.readText()
        } catch (e: IOException) {
            echoMessage(formatMessage(FindingSeverity.ERROR, "cannot read baseline '$path': ${e.message}"))
            throw ProgramResult(ExitCode.GENERAL_ERROR.code)
        }
    }

    private fun failOnBaseline(message: String): Nothing {
        echoMessage(formatMessage(FindingSeverity.ERROR, message))
        echoMessage(formatHint("omit --baseline to issue a new document"))
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
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
