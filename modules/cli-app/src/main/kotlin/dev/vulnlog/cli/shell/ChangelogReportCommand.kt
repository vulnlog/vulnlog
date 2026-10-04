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
import com.github.ajalt.clikt.parameters.types.choice
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.ChangelogFormatRequest
import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.ChangelogRequest
import dev.vulnlog.lib.app.generateChangelog
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

    val format: ChangelogFormatRequest by option(
        "--format",
        help =
            """
            Output format for the report.
            'text' (default) reads in a terminal.
            'markdown' renders one section per release, ready to paste into a changelog file.
            """.trimIndent(),
    ).choice(ChangelogFormatRequest.byToken, ignoreCase = true)
        .default(ChangelogFormatRequest.Text)

    val brief: Boolean by option(
        "--brief",
        help = "List identifiers and severity only, without descriptions and resolution details.",
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
            )

        val outcome = generateChangelog(projects, request)
        renderChangelogMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is ChangelogOutcome.Failed -> failWith(renderChangelogFailure(outcome), exitCode(outcome))
            is ChangelogOutcome.NothingFixed -> Unit
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
