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
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.lib.core.SuppressionFilter
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.collectSuppressedVulnerabilities
import dev.vulnlog.lib.core.mapToSuppression
import dev.vulnlog.lib.parse.suppression.SuppressionFile
import dev.vulnlog.lib.parse.suppression.SuppressionWriter.writeSuppressionOutput
import dev.vulnlog.lib.shell.DirectoryOutputOption
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption
import dev.vulnlog.lib.shell.OutputOption
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

        val suppressionVulns =
            collectSuppressedVulnerabilities(vulnlogFile, SuppressionFilter(filter))
        val outputSuppressions = mapToSuppression(targetReporters, suppressionVulns)
        val contents: List<SuppressionFile> = outputSuppressions.map(::writeSuppressionOutput)

        if (contents.isEmpty()) {
            echo("No suppression entries applicable; nothing written.", err = true)
            return
        }

        if (contents.size > 1 && destination !is DirectoryOutputOption) {
            val names = targetReporters.map { it.canonical() }.sorted().joinToString(", ")
            echo(
                "Error: -o requires a single reporter. Use --reporter <name>, or the input must apply to only one reporter. Found: $names.",
                err = true,
            )
            throw ProgramResult(ExitCode.GENERAL_ERROR.ordinal)
        }

        when (val resolved = destination) {
            is DirectoryOutputOption.Directory -> writeToDirectory(resolved, contents)
            is FileOutputOption.File -> writeSingleFileOutput(resolved, contents.first())
            FileOutputOption.Stdout -> echo(contents.first().content)
        }
    }

    private fun writeToDirectory(
        destination: DirectoryOutputOption.Directory,
        suppressionFiles: List<SuppressionFile>,
    ) {
        suppressionFiles.forEach { suppressionFile ->
            val outputPath: Path = destination.path.resolve(suppressionFile.fileName)
            writeSuppressionFile(
                { echo(it) },
                { echo(it, err = true) },
                outputPath,
                suppressionFile,
            )
        }
    }

    private fun writeSingleFileOutput(
        destination: FileOutputOption.File,
        suppressionFile: SuppressionFile,
    ) {
        val outputPath = destination.path
        writeSuppressionFile(
            { echo(it) },
            { echo(it, err = true) },
            outputPath,
            suppressionFile,
        )
    }
}
