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
import dev.vulnlog.cli.shell.filter.resolveFilterOrFail
import dev.vulnlog.cli.shell.reporting.sharedProjectOrFail
import dev.vulnlog.cli.shell.validation.validateInputOrFail
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.applyFilter
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.reporting.collectChangelogReleases
import dev.vulnlog.lib.core.reporting.formatChangelogMarkdown
import dev.vulnlog.lib.core.reporting.formatChangelogText
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import dev.vulnlog.lib.model.reporting.ReportingChangelogProject
import dev.vulnlog.lib.model.reporting.ReportingChangelogRelease
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.ChangelogFormatRequest
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.FileOutputOption

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
        val validated: List<ValidVulnlogProject> = inputs.map { input -> validateInputOrFail(input).project }
        val files: List<VulnlogFile> = validated.map(ValidVulnlogProject::vulnlogProjectFile)
        val project: Project = sharedProjectOrFail(files)

        val request =
            FilterRequest(
                reporter = filterOptions.reporterRequest,
                asOf = filterOptions.asOfRequest,
                tags = filterOptions.tagsRequest,
                fixedIn = fixedIn,
            )
        val filter = resolveFilterOrFail(request, files)

        val releases: List<ReportingChangelogRelease> = collectChangelogReleases(files.map { it.applyFilter(filter) })
        diagnosticSink().debug("collected ${releases.sumOf { it.entries.size }} fixes in ${releases.size} releases")

        if (releases.isEmpty()) {
            echoStatus(formatMessage(FindingSeverity.INFO, "no fixed vulnerabilities to report"))
            return
        }

        val detail = if (brief) ChangelogDetail.BRIEF else ChangelogDetail.FULL
        val changelog = ReportingChangelogProject(project, releases)
        val content =
            when (format) {
                is ChangelogFormatRequest.Text -> formatChangelogText(changelog, detail)
                is ChangelogFormatRequest.Markdown -> formatChangelogMarkdown(changelog, detail)
            }

        when (val target = output) {
            is FileOutputOption.File -> {
                writeReport(
                    { echoStatus(it) },
                    { echoMessage(it) },
                    target,
                    content,
                )
                diagnosticSink().verbose("wrote ${target.path}")
            }

            is FileOutputOption.Stdout -> echo(content)
        }
    }
}
