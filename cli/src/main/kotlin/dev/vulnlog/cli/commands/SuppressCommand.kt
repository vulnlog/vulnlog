package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.suppression.ConsoleWriter
import dev.vulnlog.suppression.FileWriter
import dev.vulnlog.suppression.OutputWriter
import dev.vulnlog.suppression.SuppressionCollectorService
import dev.vulnlog.suppression.SuppressionConfig
import dev.vulnlog.suppression.SuppressionFileInfo
import dev.vulnlog.suppression.SuppressionFilter
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

    private val config by requireObject<ConfigAndDataForSubcommand>()

    override fun run() {
        val suppressionConfig = SuppressionConfig(config.cliVersion)
        val outputWriter: OutputWriter = suppressionOutputDir?.let { FileWriter(it) } ?: ConsoleWriter(::echo)

        val suppressionCollector: SuppressionCollectorService by inject { parametersOf(suppressionConfig) }
        val suppressionTranslator by inject<SuppressionRecordTranslator>()
        val suppressionWriter: SuppressionWriter by inject { parametersOf(outputWriter) }
        val suppressionFilter by inject<SuppressionFilter>()

        val templateNameToContent: Map<SuppressionFileInfo, List<String>> =
            templateDir.listFiles()
                ?.filter { it.isFile }
                ?.associate { SuppressionFileInfo(it.nameWithoutExtension, it.extension) to it.readLines() }
                ?: emptyMap()
        if (templateNameToContent.isEmpty()) {
            error("No template files were found in: $templateDir")
        }

        val vulnsToSuppress: Set<VulnsPerBranchAndRecord> = suppressionCollector.collect(config.filteredResult!!)
        val filteredVulnsToSuppress: Set<VulnsPerBranchAndRecord> = suppressionFilter.filter(vulnsToSuppress)
        val suppressionRecord: Set<SuppressionRecord> = suppressionTranslator.translate(filteredVulnsToSuppress)
        suppressionWriter.writeSuppression(templateNameToContent, suppressionRecord)
    }
}
