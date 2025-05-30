package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.report.generateReport
import java.time.LocalDateTime

class ReportCommand : CliktCommand() {
    override fun help(context: Context): String = "Generate a Vulnlog report files."

    private val reportOutputDir by option("--output")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .required()

    private val config by requireObject<ConfigAndDataForSubcommand>()

    override fun run() {
        config.releaseBranches.withIndex().map { (index, branch) ->
            val htmlReport = generateReport(config.cliVerison, config.vulnlogs[index], branch, LocalDateTime.now())
            if (!reportOutputDir.exists()) {
                reportOutputDir.mkdirs()
            }
            htmlReport.writeFile(reportOutputDir)
        }
    }
}
