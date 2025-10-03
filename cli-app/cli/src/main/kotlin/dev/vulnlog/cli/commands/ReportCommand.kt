package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.common.SubcommandData
import dev.vulnlog.report.service.HtmlReportArguments
import dev.vulnlog.report.service.HtmlReportService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class ReportCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String = "Generate a Vulnlog report files."

    private val reportOutputDirHelpText =
        """
        Directory to write suppression files. If not specified, suppression files are written to STDOUT.
        The directory will be created if it does not exist.
        """.trimIndent()
    private val reportOutputDir: File by option("--output")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .required()
        .help(reportOutputDirHelpText)

    private val data by requireObject<SubcommandData>()
    private val htmlReportService: HtmlReportService by inject()

    override fun run() {
        val config = HtmlReportArguments(reportOutputDir)
        htmlReportService.generateReport(config, data.cliVersion, data.vulnEntriesFiltered)
    }
}
