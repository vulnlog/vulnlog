// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.SuppressionFilter
import dev.vulnlog.lib.core.buildSuppressionOutputs
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.collectSuppressedVulnerabilities
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.renderSuppressionExclusion
import dev.vulnlog.lib.core.renderSuppressionInclusions
import dev.vulnlog.lib.core.renderSuppressionWritten
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import dev.vulnlog.lib.parse.suppression.SuppressionWriter.writeSuppressionOutput
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.shell.DirectoryOutputOption
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import dev.vulnlog.lib.shell.OutputOption
import dev.vulnlog.lib.shell.SuppressionFormatRequest
import java.nio.file.Path

class SuppressCommand : CliktCommand(name = "suppress") {
    override fun help(context: Context): String = "Create suppression files."

    val input: FileInputOption by argument(
        help = "Vulnlog file, or '-' to read from stdin, to create suppression files from.",
    ).convert(conversion = ArgumentTransformContext::toInputFileOption)

    val destination: OutputOption by mutuallyExclusiveOptions(
        option(
            "-o",
            "--output",
            help =
                "Output file path, or '-' to write to stdout. " +
                    "Requires a single reporter (set --reporter, or the input must apply to only one reporter).",
        ).convert(conversion = OptionCallTransformContext::toOutputFileOption),
        option(
            "--output-dir",
            help = "Output directory for the suppression files. Defaults to the current directory.",
        ).convert(conversion = OptionCallTransformContext::toOutputDirectoryOption),
    ).single()
        .default(DirectoryOutputOption.Directory(Path.of(System.getProperty("user.dir"))))

    val format: SuppressionFormatRequest by option(
        "--format",
        help =
            """
            Output format for the suppression files.
            'auto' (default) uses each reporter's native format where one exists and falls back to the generic Vulnlog JSON format otherwise.
            'generic' forces the generic Vulnlog JSON format for every reporter.
            """.trimIndent(),
    ).choice(SuppressionFormatRequest.byToken, ignoreCase = true)
        .default(SuppressionFormatRequest.Auto)

    val filterOptions by FilterOptions()

    override fun run() {
        val parsedSuccessfully = parseInputOrFail(listOf(input))
        validateParsedInputOrFailWithFailureOutput(parsedSuccessfully)

        val vulnlogFile = parsedSuccessfully.values.first().content

        val filter = resolveFilter(filterOptions, vulnlogFile)

        val targetReporters =
            vulnlogFile.vulnerabilities
                .flatMap { it.reports }
                .map { it.reporter }
                .filter { filter.reporter == null || it == filter.reporter }
                .toSet()

        val collected = collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(filter))
        val suppressionResult = buildSuppressionOutputs(targetReporters, collected.included, format)
        (collected.exclusions + suppressionResult.exclusions).forEach { exclusion ->
            diagnosticSink().verbose(renderSuppressionExclusion(exclusion))
        }
        renderSuppressionInclusions(collected.included).forEach { diagnosticSink().debug(it) }
        val contents: List<RenderedSuppression> =
            suppressionResult.outputs.map { output -> RenderedSuppression(output, writeSuppressionOutput(output)) }

        if (contents.isEmpty()) {
            echoStatus(formatStatus(StatusVerb.UNCHANGED, "no suppression entries applicable"))
            return
        }

        if (contents.size > 1 && destination !is DirectoryOutputOption) {
            val names = targetReporters.map { it.canonical() }.sorted().joinToString(", ")
            echo(formatMessage(Severity.ERROR, "-o requires a single reporter, found: $names"), err = true)
            echo(formatHint("use --reporter <name> to pick one, or --output-dir for one file per reporter"), err = true)
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        when (val resolved = destination) {
            is DirectoryOutputOption.Directory -> writeToDirectory(resolved, contents)
            is FileOutputOption.File -> writeSingleFileOutput(resolved, contents.first())
            FileOutputOption.Stdout -> {
                echo(contents.first().file.content)
                diagnosticSink().verbose(renderSuppressionWritten("<stdout>", contents.first().output))
            }
        }
    }

    private fun writeToDirectory(
        destination: DirectoryOutputOption.Directory,
        suppressions: List<RenderedSuppression>,
    ) {
        suppressions.forEach { (output, suppressionFile) ->
            val outputPath: Path = destination.path.resolve(suppressionFile.fileName)
            writeSuppressionFile(
                { echoStatus(it) },
                { echo(it, err = true) },
                outputPath,
                suppressionFile,
            )
            diagnosticSink().verbose(renderSuppressionWritten(outputPath.toString(), output))
        }
    }

    private fun writeSingleFileOutput(
        destination: FileOutputOption.File,
        suppression: RenderedSuppression,
    ) {
        val outputPath = destination.path
        writeSuppressionFile(
            { echoStatus(it) },
            { echo(it, err = true) },
            outputPath,
            suppression.file,
        )
        diagnosticSink().verbose(renderSuppressionWritten(outputPath.toString(), suppression.output))
    }
}

private data class RenderedSuppression(
    val output: SuppressionOutput,
    val file: SuppressionFile,
)
