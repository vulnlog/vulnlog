// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
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
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.SuppressionFile
import dev.vulnlog.lib.app.SuppressionFormatRequest
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.app.SuppressionRequest
import dev.vulnlog.lib.app.generateSuppressions
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.io.DirectoryOutputOption
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.OutputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderSuppressionFailure
import dev.vulnlog.lib.render.renderSuppressionMessages
import dev.vulnlog.lib.render.renderSuppressionWritten
import java.nio.file.Path
import java.time.LocalDate

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

    val renamedFilterOptions by RenamedFilterOptions()

    override fun run() {
        val project = validateInputOrFail(input).project
        failOnRenamedFilterFlags(renamedFilterOptions)
        val request =
            SuppressionRequest(
                filter =
                    FilterRequest(
                        reporter = filterOptions.reporterRequest,
                        asOf = filterOptions.asOfRequest,
                        tags = filterOptions.tagsRequest,
                    ),
                format = format,
                today = LocalDate.now(),
                singleFile = destination !is DirectoryOutputOption,
            )

        val outcome = generateSuppressions(project, request)
        renderSuppressionMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is SuppressionOutcome.Failed ->
                failWith(renderSuppressionFailure(outcome, "-o", "--reporter", "--output-dir"), exitCode(outcome))

            is SuppressionOutcome.NothingToSuppress -> Unit

            is SuppressionOutcome.Generated -> write(outcome.files)
        }
    }

    /** A single-file target gets exactly one file: the use case fails on several. */
    private fun write(files: List<SuppressionFile>) {
        when (val target = destination) {
            is DirectoryOutputOption.Directory ->
                files.forEach { file -> write(target.path.resolve(file.fileName), file) }

            is FileOutputOption.File -> write(target.path, files.single())

            FileOutputOption.Stdout -> {
                echo(files.single().content)
                echoMessage(renderSuppressionWritten("<stdout>", files.single()))
            }
        }
    }

    private fun write(
        path: Path,
        file: SuppressionFile,
    ) {
        writeOrFail(writeOutput(path, file.content))
        echoMessage(Message.Status(formatStatus(StatusVerb.WROTE, path.toString())))
        echoMessage(renderSuppressionWritten(path.toString(), file))
    }
}
