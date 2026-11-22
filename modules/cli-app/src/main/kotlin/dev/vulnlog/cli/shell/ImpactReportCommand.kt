// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.AnsiLevel
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.report.ImpactReportFormat
import dev.vulnlog.lib.app.report.ImpactReportOutcome
import dev.vulnlog.lib.app.report.ImpactReportRequest
import dev.vulnlog.lib.app.report.generateImpactReport
import dev.vulnlog.lib.codec.impact.TextLayout
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.FileOutputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderImpactReportFailure
import dev.vulnlog.lib.render.renderImpactReportMessages
import dev.vulnlog.lib.render.renderWritten
import java.nio.file.Path
import java.time.Instant

class ImpactReportCommand : CliktCommand(name = "impact") {
    override fun help(context: Context): String =
        "Generate a report of how vulnerabilities affect this project, as HTML, JSON or a table. " +
            "Several Vulnlog files are merged into one report and must share the same project metadata."

    override fun helpEpilog(context: Context): String =
        """
        |Examples:
        |
        |Report every entry in the file.
        |
        |vulnlog report impact vulnlog.yaml
        |
        |Report the state as of release 1.1.0, which includes every earlier release.
        |
        |vulnlog report impact vulnlog.yaml --as-of 1.1.0
        |
        |Merge several files of the same project into one report.
        |
        |vulnlog report impact frontend.vl.yaml backend.vl.yaml
        |
        |Write the report as JSON to a file.
        |
        |vulnlog report impact vulnlog.yaml --format json > impact.json
        |
        |Show the report as a table in the terminal.
        |
        |vulnlog report impact vulnlog.yaml --format table
        """.trimMargin()

    val inputs: List<FileInputOption> by vulnlogFileInputs(
        "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    )

    val output: FileOutputOption? by option(
        "-o",
        "--output",
        metavar = "<path>",
        help =
            "Output file path, or '-' to write to stdout. Defaults to vulnlog-impact-report.html in the " +
                "current directory for html, and to stdout for json and table.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)

    val format: ImpactReportFormat by option(
        "--format",
        metavar = "<format>",
        help = "Output format: html, json or table. Defaults to html.",
    ).enum<ImpactReportFormat> { it.token }
        .default(ImpactReportFormat.HTML)

    val filterOptions by FilterOptions()

    val renamedFilterOptions by RenamedFilterOptions()

    val impactFilterOptions by ImpactFilterOptions()

    override fun run() {
        val projects = inputs.map { input -> validateInputOrFail(input).project }
        failOnRenamedFilterFlags(renamedFilterOptions)
        val target = resolveOutput()
        val request =
            ImpactReportRequest(
                filter =
                    FilterRequest(
                        reporter = filterOptions.reporterRequest,
                        asOf = filterOptions.asOfRequest,
                        tags = filterOptions.tagsRequest,
                        states = impactFilterOptions.statesRequest,
                        verdicts = impactFilterOptions.verdictsRequest,
                        dispositions = impactFilterOptions.dispositionsRequest,
                    ),
                generatedAt = Instant.now(),
                vulnlogVersion = BuildInfo.VERSION,
                format = format,
                textLayout = resolveTextLayout(target),
            )

        val outcome = generateImpactReport(projects, request)
        renderImpactReportMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is ImpactReportOutcome.Failed -> failWith(renderImpactReportFailure(outcome), exitCode(outcome))
            is ImpactReportOutcome.Generated -> write(outcome.content, target)
        }
    }

    /** HTML is opened in a browser, so it goes to a file. Data formats go to stdout, so they can be piped. */
    private fun resolveOutput(): FileOutputOption =
        output ?: when (format) {
            ImpactReportFormat.HTML ->
                FileOutputOption.File(Path.of(System.getProperty("user.dir"), "vulnlog-impact-report.html"))
            ImpactReportFormat.JSON, ImpactReportFormat.TABLE -> FileOutputOption.Stdout
        }

    /**
     * Only a terminal gets its own width and colours. A file or a pipe gets the same bytes, whoever runs the command.
     */
    private fun resolveTextLayout(target: FileOutputOption): TextLayout {
        val info = terminal.terminalInfo
        if (target !is FileOutputOption.Stdout || !info.outputInteractive) return TextLayout.PLAIN
        return TextLayout(width = terminal.size.width, colors = info.ansiLevel != AnsiLevel.NONE)
    }

    private fun write(
        content: String,
        target: FileOutputOption,
    ) {
        when (target) {
            is FileOutputOption.File -> {
                writeOrFail(writeOutput(target.path, content))
                echoMessage(Message.Status(formatStatus(StatusVerb.WROTE, target.path.toString())))
                echoMessage(renderWritten(target.path.toString()))
            }
            is FileOutputOption.Stdout -> echo(content, trailingNewline = !content.endsWith("\n"))
        }
    }
}
