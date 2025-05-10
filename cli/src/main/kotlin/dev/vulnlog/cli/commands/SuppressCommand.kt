package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.suppression.SuppressionCollectorService
import dev.vulnlog.suppression.SuppressionConfig
import dev.vulnlog.suppression.SuppressionRecord
import dev.vulnlog.suppression.SuppressionRecordTranslator
import dev.vulnlog.suppression.SuppressionWriter
import dev.vulnlog.suppression.VulnsPerBranchAndRecord
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class SuppressCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String = "Generate a Vulnlog suppression files."

    private val templateDir by option("--template-dir")
        .file(mustExist = false, canBeDir = true, canBeFile = false)
        .required()

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
        val suppressionConfig = SuppressionConfig(config.cliVersion, templateDir)

        val suppressionCollector: SuppressionCollectorService by inject { parametersOf(suppressionConfig) }
        val suppressionTranslator by inject<SuppressionRecordTranslator>()
        val suppressionWriter: SuppressionWriter by inject { parametersOf(suppressionOutputDir) }

        val vulnsToSuppress: Set<VulnsPerBranchAndRecord> = suppressionCollector.collect(config.filteredResult!!)
        val suppressionRecord: Set<SuppressionRecord> = suppressionTranslator.translate(vulnsToSuppress)
        suppressionWriter.writeSuppression(suppressionRecord)
    }
}
