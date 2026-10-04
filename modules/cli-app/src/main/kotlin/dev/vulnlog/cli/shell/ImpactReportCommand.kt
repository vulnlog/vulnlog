// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.OptionCallTransformContext
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.ImpactReportRequest
import dev.vulnlog.lib.app.generateImpactReport
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
        "Generate an HTML report of how vulnerabilities affect this project. " +
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
        """.trimMargin()

    val inputs: List<FileInputOption> by vulnlogFileInputs(
        "Vulnlog file(s), or '-' to read from stdin, to create the report from.",
    )

    val output: FileOutputOption by option(
        "-o",
        "--output",
        metavar = "<path>",
        help =
            "Output file path, or '-' to write to stdout. " +
                "Defaults to vulnlog-impact-report.html in the current directory.",
    ).convert(conversion = OptionCallTransformContext::toOutputFileOption)
        .default(FileOutputOption.File(Path.of("vulnlog-impact-report.html")))

    val filterOptions by FilterOptions()

    val renamedFilterOptions by RenamedFilterOptions()

    val impactFilterOptions by ImpactFilterOptions()

    override fun run() {
        val projects = inputs.map { input -> validateInputOrFail(input).project }
        failOnRenamedFilterFlags(renamedFilterOptions)
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
            )

        val outcome = generateImpactReport(projects, request)
        renderImpactReportMessages(outcome).forEach(::echoMessage)
        when (outcome) {
            is ImpactReportOutcome.Failed -> failWith(renderImpactReportFailure(outcome), exitCode(outcome))
            is ImpactReportOutcome.Generated -> write(outcome.content)
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
