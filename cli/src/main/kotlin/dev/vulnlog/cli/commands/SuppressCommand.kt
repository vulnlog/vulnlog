package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.report.generateReport
import dev.vulnlog.suppression.SuppressionConfig
import dev.vulnlog.suppression.SuppressionGenerator
import dev.vulnlog.suppression.SuppressionWriter
import dev.vulnlog.suppression.WritableSuppression
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime

class SuppressCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String = "Generate a Vulnlog suppression files."

    private val templateDir by option("--template-dir")
        .file(mustExist = false, canBeDir = true, canBeFile = false)

    private val suppressionOutputDir by option("--output")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .required()

    private val config by requireObject<ConfigAndDataForSubcommand>()

    override fun run() {
        /*
         * Generate suppression files containing all suppressed vulnerabilities per branch and report.
         * One release branch can have zero, one or multiple suppression files.
         * One vulnerability can be in zero, one or multiple suppression files.
         * Per release branch and reporter only one suppression file is generated.
         *
         * Task:
         * Load templates, if non is available fail
         * Write suppression files per release branch in output directory
         */
        val suppressionConfig =
            SuppressionConfig(config.cliVerison, config.filteredResult, templateDir)

        val suppressionGenerator: SuppressionGenerator by inject { parametersOf(suppressionConfig) }
        val suppressionWriter: SuppressionWriter by inject { parametersOf(suppressionOutputDir) }

        val generatedSuppressions: Set<WritableSuppression> = suppressionGenerator.generateSuppressions()
        suppressionWriter.writeSuppression(generatedSuppressions)

        config.releaseBranches.withIndex().map { (index, branch) ->
            val htmlReport = generateReport(config.cliVerison, config.vulnlogs[index], branch, LocalDateTime.now())
            if (!suppressionOutputDir.exists()) {
                suppressionOutputDir.mkdirs()
            }
            htmlReport.writeFile(suppressionOutputDir)
        }
    }
}
