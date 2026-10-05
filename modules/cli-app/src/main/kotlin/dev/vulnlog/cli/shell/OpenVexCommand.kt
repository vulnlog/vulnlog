// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
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
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.OpenVexRequest
import dev.vulnlog.lib.app.generateOpenVex
import dev.vulnlog.lib.codec.openvex.openVexDocumentId
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.readOpenVexBaseline
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderOpenVexBaselineFailure
import dev.vulnlog.lib.render.renderOpenVexFailure
import dev.vulnlog.lib.render.renderOpenVexMessages
import dev.vulnlog.lib.render.renderOpenVexWritten
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

private const val BASELINE_OPTION = "--baseline"
private const val RELEASE_OPTION = "--release"

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
        RELEASE_OPTION,
        metavar = "<release-id>",
        help =
            """
            Write the document for this release only, even one without published_at.
            Without it the document covers every published release that declares purls.
            """.trimIndent(),
    )

    val tagsRequest: Set<String> by option(
        "--tag",
        metavar = "<tag>",
        help =
            """
            Keep only the vulnerabilities and release purls carrying this tag.
            Use multiple times to keep those carrying any of them.
            """.trimIndent(),
    ).multiple()
        .unique()

    val baselineRequest: Path? by option(
        BASELINE_OPTION,
        metavar = "<path>",
        help =
            """
            Previous OpenVEX document to continue. Its '@id' stays; 'timestamp' and 'version' move on only when the content changed.
            Without a baseline every run issues a new document.
            """.trimIndent(),
    ).path(canBeDir = false)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to vex.json in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vex.json")))

    /** Becomes a `--format-version` option once a second OpenVEX version is supported. */
    private val formatVersion: OpenVexFormatVersion = OpenVexFormatVersion.LATEST

    override fun run() {
        val project = validateInputOrFail(input).project
        val request =
            OpenVexRequest(
                release = releaseRequest,
                tags = tagsRequest,
                baseline = baselineRequest?.let(::readBaselineOrFail),
                documentId = openVexDocumentId(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = OpenVexTooling("CLI", BuildInfo.VERSION),
                formatVersion = formatVersion,
            )

        val outcome = generateOpenVex(project, request)
        renderOpenVexMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is OpenVexOutcome.Failed -> {
                val baseline = baselineRequest?.toString().orEmpty()
                failWith(renderOpenVexFailure(outcome, baseline, BASELINE_OPTION, RELEASE_OPTION), exitCode(outcome))
            }

            is OpenVexOutcome.Generated -> write(outcome)
        }
    }

    private fun write(outcome: OpenVexOutcome.Generated) {
        when (val target = output) {
            is FileOutputOption.File -> {
                if (outcome is OpenVexOutcome.Unchanged && isBaselinePath(target.path)) {
                    echoMessage(Message.Status(formatStatus(StatusVerb.UNCHANGED, target.path.toString())))
                    return
                }
                writeOrFail(writeOutput(target.path, outcome.content))
                echoMessage(Message.Status(formatStatus(StatusVerb.WROTE, target.path.toString())))
                echoMessage(renderOpenVexWritten(target.path.toString(), outcome))
            }

            FileOutputOption.Stdout -> {
                echo(outcome.content, trailingNewline = false)
                echoMessage(renderOpenVexWritten("<stdout>", outcome))
            }
        }
    }

    private fun isBaselinePath(target: Path): Boolean =
        baselineRequest?.toAbsolutePath()?.normalize() == target.toAbsolutePath().normalize()

    /** A missing file is an error: the user asked to continue a document, and a new one would fork its identity. */
    private fun readBaselineOrFail(path: Path): String =
        when (val read = readOpenVexBaseline(path)) {
            is OpenVexBaselineRead.Present -> read.text
            is OpenVexBaselineRead.Unavailable ->
                failWith(listOf(renderOpenVexBaselineFailure(read, path.toString(), BASELINE_OPTION)), exitCode(read))
        }
}
