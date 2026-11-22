// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.report.ChangelogFormat
import dev.vulnlog.lib.app.report.ChangelogOutcome
import dev.vulnlog.lib.app.report.ChangelogRequest
import dev.vulnlog.lib.app.report.generateChangelog
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderChangelogFailure
import dev.vulnlog.lib.render.renderChangelogMessages
import dev.vulnlog.lib.render.renderWritten
import java.time.Instant

class ChangelogReportCommand : CliktCommand(name = "changelog") {
    override fun help(context: Context): String =
        "Generate a report of which vulnerabilities each release fixed. " +
            "Several Vulnlog files are merged into one report and must share the same project metadata."

    override fun helpEpilog(context: Context): String =
        """
        |Examples:
        |
        |Report every release that shipped a fix.
        |
        |vulnlog report changelog vulnlog.yaml
        |
        |Report the release that is about to ship, ready to paste into a changelog file.
        |
        |vulnlog report changelog vulnlog.yaml --fixed-in 1.2.0 --format markdown
        |
        |Write the changelog as JSON for another tool.
        |
        |vulnlog report changelog vulnlog.yaml --format json -o changelog.json
        |
        |Merge several files of the same project into one report.
        |
        |vulnlog report changelog frontend.vl.yaml backend.vl.yaml
        """.trimMargin()

    val inputs: List<FileInputOption> by vulnlogFileInputs(
        "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    )

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help = "Output file path, or '-' to write to stdout. Defaults to stdout.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.Stdout)

    val fixedIn: String? by option(
        "--fixed-in",
        metavar = "<release-id>",
        help = "Report only the vulnerabilities this release shipped a fix for.",
    )

    val format: ChangelogFormat by option(
        "--format",
        help =
            """
            Output format for the report.
            'text' (default) reads in a terminal.
            'markdown' renders one section per release, ready to paste into a changelog file.
            'json' holds every field and the metadata of the run, for other tools to read.
            """.trimIndent(),
    ).enum<ChangelogFormat> { it.token }
        .default(ChangelogFormat.TEXT)

    val brief: Boolean by option(
        "--brief",
        help =
            "List identifiers and severity only, without descriptions and resolution details. " +
                "Applies to text and markdown. JSON always holds every field.",
    ).flag()

    val filterOptions by FilterOptions()

    override fun run() {
        val projects = inputs.map { input -> validateInputOrFail(input).project }
        val request =
            ChangelogRequest(
                filter =
                    FilterRequest(
                        reporter = filterOptions.reporterRequest,
                        asOf = filterOptions.asOfRequest,
                        tags = filterOptions.tagsRequest,
                        fixedIn = fixedIn,
                    ),
                format = format,
                detail = if (brief) ChangelogDetail.BRIEF else ChangelogDetail.FULL,
                generatedAt = Instant.now(),
                vulnlogVersion = BuildInfo.VERSION,
            )

        val outcome = generateChangelog(projects, request)
        renderChangelogMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is ChangelogOutcome.Failed -> failWith(renderChangelogFailure(outcome), exitCode(outcome))
            // A tool that reads JSON needs a document, also one without releases. Text and markdown stay empty.
            is ChangelogOutcome.NothingFixed -> if (format == ChangelogFormat.JSON) write(outcome.content)
            is ChangelogOutcome.Fixed -> write(outcome.content)
        }
    }

    private fun write(content: String) {
        when (val target = output) {
            is FileOutputOption.File -> {
                writeOrFail(writeOutput(target.path, content))
                echoMessage(Message.Status(formatStatus(StatusVerb.WROTE, target.path.toString())))
                echoMessage(renderWritten(target.path.toString()))
            }

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
