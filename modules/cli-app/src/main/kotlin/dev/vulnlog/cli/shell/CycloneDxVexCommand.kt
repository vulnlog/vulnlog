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
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.path
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.CycloneDxVexOutcome
import dev.vulnlog.lib.app.CycloneDxVexRequest
import dev.vulnlog.lib.app.generateCycloneDxVex
import dev.vulnlog.lib.codec.cyclonedx.cycloneDxSerialNumber
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.readVexBaseline
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.vex.VexBaselineRead
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderCycloneDxVexFailure
import dev.vulnlog.lib.render.renderCycloneDxVexMessages
import dev.vulnlog.lib.render.renderCycloneDxVexWritten
import dev.vulnlog.lib.render.renderVexBaselineFailure
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

private const val BASELINE_OPTION = "--baseline"
private const val TAG_OPTION = "--tag"

class CycloneDxVexCommand : CliktCommand(name = "cyclonedx") {
    override fun help(context: Context): String =
        "Generate CycloneDX VEX files from Vulnlog files, one release per document. (Incubating feature)"

    override fun helpEpilog(context: Context): String =
        """
        |Examples:
        |
        |Write the document for release 1.2.0 to vex.cdx.json in the current directory.
        |
        |vulnlog vex cyclonedx vulnlog.yaml --release 1.2.0
        |
        |Write the document to stdout.
        |
        |vulnlog vex cyclonedx vulnlog.yaml --release 1.2.0 -o -
        |
        |Write the document for the container image of release 1.2.0, to upload to its Dependency-Track project.
        |
        |vulnlog vex cyclonedx vulnlog.yaml --release 1.2.0 --tag container -o vex-1.2.0-container.cdx.json
        |
        |Continue an existing document, so its serial number stays stable and the version counts up.
        |
        |vulnlog vex cyclonedx vulnlog.yaml --release 1.2.0 --baseline vex.cdx.json -o vex.cdx.json
        """.trimMargin()

    val input: FileInputOption by argument(
        help = "Vulnlog file, or '-' to read from stdin.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)

    val releaseRequest: String by option(
        "--release",
        metavar = "<release-id>",
        help =
            """
            Write the document for this release, even one without published_at.
            CycloneDX holds one analysis per vulnerability, so a document covers one release.
            """.trimIndent(),
    ).required()

    val tagsRequest: Set<String> by option(
        TAG_OPTION,
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
            Previous CycloneDX VEX document to continue. Its 'serialNumber' stays; 'metadata.timestamp' and 'version' move on only when the content changed.
            Without a baseline every run issues a new document.
            """.trimIndent(),
    ).path(canBeDir = false)

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to vex.cdx.json in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vex.cdx.json")))

    /** Becomes a `--format-version` option once a second CycloneDX version is supported. */
    private val formatVersion: CycloneDxVexFormatVersion = CycloneDxVexFormatVersion.LATEST

    override fun run() {
        val project = validateInputOrFail(input).project
        val request =
            CycloneDxVexRequest(
                release = releaseRequest,
                tags = tagsRequest,
                baseline = baselineRequest?.let(::readBaselineOrFail),
                serialNumber = cycloneDxSerialNumber(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = VexTooling("CLI", BuildInfo.VERSION),
                formatVersion = formatVersion,
            )

        val outcome = generateCycloneDxVex(project, request)
        renderCycloneDxVexMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is CycloneDxVexOutcome.Failed -> {
                val baseline = baselineRequest?.toString().orEmpty()
                failWith(renderCycloneDxVexFailure(outcome, baseline, BASELINE_OPTION, TAG_OPTION), exitCode(outcome))
            }

            is CycloneDxVexOutcome.Generated -> write(outcome)
        }
    }

    private fun write(outcome: CycloneDxVexOutcome.Generated) {
        when (val target = output) {
            is FileOutputOption.File -> {
                if (outcome is CycloneDxVexOutcome.Unchanged && isBaselinePath(target.path)) {
                    echoMessage(Message.Status(formatStatus(StatusVerb.UNCHANGED, target.path.toString())))
                    return
                }
                writeOrFail(writeOutput(target.path, outcome.content))
                echoMessage(Message.Status(formatStatus(StatusVerb.WROTE, target.path.toString())))
                echoMessage(renderCycloneDxVexWritten(target.path.toString(), outcome))
            }

            FileOutputOption.Stdout -> {
                echo(outcome.content, trailingNewline = false)
                echoMessage(renderCycloneDxVexWritten("<stdout>", outcome))
            }
        }
    }

    private fun isBaselinePath(target: Path): Boolean =
        baselineRequest?.toAbsolutePath()?.normalize() == target.toAbsolutePath().normalize()

    /** A missing file is an error: the user asked to continue a document, and a new one would fork its identity. */
    private fun readBaselineOrFail(path: Path): String =
        when (val read = readVexBaseline(path)) {
            is VexBaselineRead.Present -> read.text
            is VexBaselineRead.Unavailable ->
                failWith(listOf(renderVexBaselineFailure(read, path.toString(), BASELINE_OPTION)), exitCode(read))
        }
}
